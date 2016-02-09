/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package at.bitfire.davdroid.syncadapter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SyncResult;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.util.Log;

import net.fortuna.ical4j.model.ValidationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import at.bitfire.davdroid.ArrayUtils;
import at.bitfire.davdroid.crypto.KeyManager;
import at.bitfire.davdroid.resource.Event;
import at.bitfire.davdroid.resource.LocalCollection;
import at.bitfire.davdroid.resource.LocalStorageException;
import at.bitfire.davdroid.resource.RecordNotFoundException;
import at.bitfire.davdroid.resource.RemoteCollection;
import at.bitfire.davdroid.resource.Resource;
import at.bitfire.davdroid.webdav.DavException;
import at.bitfire.davdroid.webdav.HttpException;
import at.bitfire.davdroid.webdav.NotFoundException;
import at.bitfire.davdroid.webdav.PreconditionFailedException;

public class SyncManager {
	private static final String TAG = "davdroid.SyncManager";
	
	private static final int MAX_MULTIGET_RESOURCES = 35;

	protected LocalCollection<? extends Resource> local;
	protected RemoteCollection<? extends Resource> remote;
    protected String user;
	
	
	public SyncManager(LocalCollection<? extends Resource> local, RemoteCollection<? extends Resource> remote, String accountName) {
		this.local = local;
		this.remote = remote;
        this.user = accountName;
	}


    public boolean synchronizeKeys(boolean afterFetch) throws LocalStorageException {

        Event event = (Event) local.findByRealName(KeyManager.KEY_STORAGE_EVENT_NAME,true);

        KeyManager keyManager = KeyManager.getInstance();

        if (event != null) {
            Log.i(TAG, "Found KeyManager event");
            event.description = keyManager.initKeyBank(user,event.description);
            if (KeyManager.getInstance().isUpdated()) {
                Log.i(TAG, "Updating KeyManager event");
                local.updateKeyManager(event);
                local.commit();
//                KeyManager.getInstance().setUpdated(false);
                return true;
            }

        } else if (afterFetch) {
            Log.i(TAG, "Adding KeyManager event");
            event = new Event(null,null);
            event.initialize();
            event.summary = KeyManager.KEY_STORAGE_EVENT_NAME;
            // Generates a new key bank
            event.description = keyManager.initKeyBank(user,null);

            try {
                SimpleDateFormat sdf = new SimpleDateFormat(KeyManager.EVENT_TIME_FORMAT);
                event.setDtStart(sdf.parse(KeyManager.KEY_STORAGE_EVENT_TIME).getTime(), null);
                event.setDtEnd(sdf.parse(KeyManager.KEY_STORAGE_EVENT_TIME_END).getTime(), null);
            } catch (ParseException e) {
               Log.e(TAG, e.toString());
            }

            local.add(event,true);

            local.commit();
            Log.i(TAG, "KeyManager event added");
            return true;
        }
        return false;

    }

    public void synchronize(boolean manualSync, SyncResult syncResult) throws URISyntaxException, LocalStorageException, IOException, HttpException, DavException {


		// PHASE 1: push local changes to server
		int	deletedRemotely = pushDeleted(),
			addedRemotely = pushNew(),
			updatedRemotely = pushDirty();
		
		syncResult.stats.numEntries = deletedRemotely + addedRemotely + updatedRemotely;
		
		// PHASE 2A: check if there's a reason to do a sync with remote (= forced sync or remote CTag changed)
		boolean fetchCollection = syncResult.stats.numEntries > 0;
		if (manualSync) {
			Log.i(TAG, "Synchronization forced");
			fetchCollection = true;
		}
		if (!fetchCollection) {
			String	currentCTag = remote.getCTag(),
					lastCTag = local.getCTag();
			Log.d(TAG, "Last local CTag = " + lastCTag + "; current remote CTag = " + currentCTag);
			if (currentCTag == null || !currentCTag.equals(lastCTag))
				fetchCollection = true;
		}


		if (!fetchCollection ) {
            return;
		}
		
		// PHASE 2B: detect details of remote changes
		Log.i(TAG, "Fetching remote resource list");
		Set<Resource>	remotelyAdded = new HashSet<Resource>(),
						remotelyUpdated = new HashSet<Resource>();
		
		Resource[] remoteResources = remote.getMemberETags();
		for (Resource remoteResource : remoteResources) {
			try {
				Resource localResource = local.findByRemoteName(remoteResource.getName(), false);
                Log.i(TAG,"Local Resource ETag " + localResource.getETag());
				if (localResource.getETag() == null || !localResource.getETag().equals(remoteResource.getETag()))
					remotelyUpdated.add(remoteResource);
			} catch(RecordNotFoundException e) {
				remotelyAdded.add(remoteResource);
			}
		}

        if (remotelyAdded.toArray(new Resource[0]).length > 0)
            syncKeyManager(remotelyAdded.toArray(new Resource[0]));
        if (remotelyUpdated.toArray(new Resource[0]).length > 0)
            syncKeyManager(remotelyUpdated.toArray(new Resource[0]));

		// PHASE 3: pull remote changes from server
		syncResult.stats.numInserts = pullNew(remotelyAdded.toArray(new Resource[0]));
		syncResult.stats.numUpdates = pullChanged(remotelyUpdated.toArray(new Resource[0]));
		syncResult.stats.numEntries += syncResult.stats.numInserts + syncResult.stats.numUpdates;
		
		Log.i(TAG, "Removing non-dirty resources that are not present remotely anymore");
		local.deleteAllExceptRemoteNames(remoteResources);
		local.commit();

		// update collection CTag
		Log.i(TAG, "Sync complete, fetching new CTag " + remote.getCTag());
		local.setCTag(remote.getCTag());


	}



	
	private int pushDeleted() throws URISyntaxException, LocalStorageException, IOException, HttpException {
		int count = 0;
		long[] deletedIDs = local.findDeleted();
		
		try {
			Log.i(TAG, "Remotely removing " + deletedIDs.length + " deleted resource(s) (if not changed)");
			for (long id : deletedIDs)
				try {
					Resource res = local.findById(id, false);
					if (res.getName() != null)	// is this resource even present remotely?
						try {
							remote.delete(res);
						} catch(NotFoundException e) {
							Log.i(TAG, "Locally-deleted resource has already been removed from server");
						} catch(PreconditionFailedException e) {
							Log.i(TAG, "Locally-deleted resource has been changed on the server in the meanwhile");
						}
					
					// always delete locally so that the record with the DELETED flag doesn't cause another deletion attempt
					local.delete(res);
					
					count++;
				} catch (RecordNotFoundException e) {
					Log.wtf(TAG, "Couldn't read locally-deleted record", e);
				}
		} finally {
			local.commit();
		}
		return count;
	}
	
	private int pushNew() throws URISyntaxException, LocalStorageException, IOException, HttpException {
		int count = 0;
		long[] newIDs = local.findNew();
		Log.i(TAG, "Uploading " + newIDs.length + " new resource(s) (if not existing)");
		try {
			for (long id : newIDs)
				try {
					Resource res = local.findById(id, true);

					String eTag = remote.add(res);
                    Log.i(TAG,"Updating with eTag " + eTag);
					if (eTag != null)
						local.updateETag(res, eTag);
					local.clearDirty(res);
					count++;
				} catch(PreconditionFailedException e) {
					Log.i(TAG, "Didn't overwrite existing resource with other content");
				} catch (ValidationException e) {
					Log.e(TAG, "Couldn't create entity for adding: " + e.toString());
				} catch (RecordNotFoundException e) {
					Log.wtf(TAG, "Couldn't read new record", e);
				}
		} finally {
			local.commit();
		}
		return count;
	}
	
	private int pushDirty() throws URISyntaxException, LocalStorageException, IOException, HttpException {

		int count = 0;
		long[] dirtyIDs = local.findUpdated();
		Log.i(TAG, "Uploading " + dirtyIDs.length + " modified resource(s) (if not changed)");
		try {
			for (long id : dirtyIDs) {
				try {
					Resource res = local.findById(id, true);
					String eTag = remote.update(res);
                    Log.i(TAG,"Updating eTag " + eTag);
					if (eTag != null)
						local.updateETag(res, eTag);
					local.clearDirty(res);
					count++;
				} catch(PreconditionFailedException e) {
					Log.i(TAG, "Locally changed resource has been changed on the server in the meanwhile");
//                    Resource res = local.findById(id, true);
//                    if (res != null) {
//                        local.clearDirty(res);
//                    }
				} catch (ValidationException e) {
					Log.e(TAG, "Couldn't create entity for updating: " + e.toString());
				} catch (RecordNotFoundException e) {
					Log.e(TAG, "Couldn't read dirty record", e);
				}
			}
		} finally {
			local.commit();
		}
		return count;
	}

    private void syncKeyManager(Resource[] resourcesToAdd) throws URISyntaxException, LocalStorageException, IOException, HttpException, DavException {
        //int count = 0;
        Log.i(TAG, "Fetching " + resourcesToAdd.length + " when trying to find KeyBank");

        for (Resource[] resources : ArrayUtils.partition(resourcesToAdd, MAX_MULTIGET_RESOURCES))
            for (Resource res : remote.multiGet(resources, false)) {

                try {
                    Log.d(TAG, "Reading KeyManager " + res.getName());
                    Log.d(TAG, "Reading KeyManager Summary" + ((Event)res).summary);
                    Log.d(TAG, "Reading KeyManager Description" + ((Event)res).description);
                    if (KeyManager.isKeyManagerEvent((Event) res)) {
                        Log.d(TAG, "Initing KeyBank " + res.getName());
                        Event keyBank = ((Event)res);
                        keyBank.description = KeyManager.getInstance().initKeyBank(user,keyBank.description);

                        if (KeyManager.getInstance().isUpdated()) {
                            String eTag = remote.update(keyBank);
                            if (eTag != null)
                                local.updateETag(res, eTag);
                            local.clearDirty(res);
                            KeyManager.getInstance().setUpdated(false);
                        }
                    }
                } catch (Exception e) {
                }
            }

    }

	private int pullNew(Resource[] resourcesToAdd) throws URISyntaxException, LocalStorageException, IOException, HttpException, DavException {
		int count = 0;
		Log.i(TAG, "Fetching " + resourcesToAdd.length + " new remote resource(s)");
		
		for (Resource[] resources : ArrayUtils.partition(resourcesToAdd, MAX_MULTIGET_RESOURCES))
			for (Resource res : remote.multiGet(resources)) {
				Log.d(TAG, "Adding " + res.getName());
				local.add(res);
				local.commit();
				count++;
			}
		return count;
	}
	
	private int pullChanged(Resource[] resourcesToUpdate) throws URISyntaxException, LocalStorageException, IOException, HttpException, DavException {
		int count = 0;
		Log.i(TAG, "Fetching " + resourcesToUpdate.length + " updated remote resource(s)");
		
		for (Resource[] resources : ArrayUtils.partition(resourcesToUpdate, MAX_MULTIGET_RESOURCES))
			for (Resource res : remote.multiGet(resources)) {
				Log.i(TAG, "Updating " + res.getName());
				local.updateByRemoteName(res);
				local.commit();
				count++;
			}
		return count;
	}

}
