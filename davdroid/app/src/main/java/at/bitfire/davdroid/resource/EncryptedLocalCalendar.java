package at.bitfire.davdroid.resource;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Entity;
import android.content.EntityIterator;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.util.Log;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;

import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.bitfire.davdroid.DateUtils;
import lombok.Cleanup;
import lombok.Getter;

/**
 * Created by jonatan on 25/11/15.
 */
public class EncryptedLocalCalendar extends LocalCollection<EncryptedEvent> {

    private static final String TAG = "davdroid.LocalCalendar";

    @Getter
    protected long id;
    @Getter protected String url;

    protected static String COLLECTION_COLUMN_CTAG = CalendarContract.Calendars.CAL_SYNC1;


	/* database fields */

    @Override
    protected Uri entriesURI() {
        return syncAdapterURI(CalendarContract.Events.CONTENT_URI);
    }

    protected String entryColumnAccountType()	{ return CalendarContract.Events.ACCOUNT_TYPE; }
    protected String entryColumnAccountName()	{ return CalendarContract.Events.ACCOUNT_NAME; }

    protected String entryColumnParentID()		{ return CalendarContract.Events.CALENDAR_ID; }
    protected String entryColumnID()			{ return CalendarContract.Events._ID; }
    protected String entryColumnRemoteName()	{ return CalendarContract.Events._SYNC_ID; }
    protected String entryColumnETag()			{ return CalendarContract.Events.SYNC_DATA1; }

    protected String entryColumnDirty()			{ return CalendarContract.Events.DIRTY; }
    protected String entryColumnDeleted()		{ return CalendarContract.Events.DELETED; }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected String entryColumnUID() {
        return (android.os.Build.VERSION.SDK_INT >= 17) ?
                CalendarContract.Events.UID_2445 : CalendarContract.Events.SYNC_DATA2;
    }


	/* class methods, constructor */

    @SuppressLint("InlinedApi")
    public static void create(Account account, ContentResolver resolver, ServerInfo.ResourceInfo info) throws LocalStorageException {
        ContentProviderClient client = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY);
        if (client == null)
            throw new LocalStorageException("No Calendar Provider found (Calendar app disabled?)");

        int color = 0xFFC3EA6E;		// fallback: "DAVdroid green"
        if (info.getColor() != null) {
            Pattern p = Pattern.compile("#?(\\p{XDigit}{6})(\\p{XDigit}{2})?");
            Matcher m = p.matcher(info.getColor());
            if (m.find()) {
                int color_rgb = Integer.parseInt(m.group(1), 16);
                int color_alpha = m.group(2) != null ? (Integer.parseInt(m.group(2), 16) & 0xFF) : 0xFF;
                color = (color_alpha << 24) | color_rgb;
            }
        }

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, account.type);
        values.put(CalendarContract.Calendars.NAME, info.getURL());
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, info.getTitle());
        values.put(CalendarContract.Calendars.CALENDAR_COLOR, color);
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        values.put(CalendarContract.Calendars.VISIBLE, 1);
        values.put(CalendarContract.Calendars.ALLOWED_REMINDERS, CalendarContract.Reminders.METHOD_ALERT);

        if (info.isReadOnly())
            values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_READ);
        else {
            values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
            values.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1);
            values.put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1);
        }

        if (android.os.Build.VERSION.SDK_INT >= 15) {
            values.put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY + "," + CalendarContract.Events.AVAILABILITY_FREE + "," + CalendarContract.Events.AVAILABILITY_TENTATIVE);
            values.put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, CalendarContract.Attendees.TYPE_NONE + "," + CalendarContract.Attendees.TYPE_OPTIONAL + "," + CalendarContract.Attendees.TYPE_REQUIRED + "," + CalendarContract.Attendees.TYPE_RESOURCE);
        }

        if (info.getTimezone() != null)
            values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, info.getTimezone());

        Log.i(TAG, "Inserting calendar: " + values.toString() + " -> " + calendarsURI(account).toString());
        try {
            client.insert(calendarsURI(account), values);
        } catch(RemoteException e) {
            throw new LocalStorageException(e);
        }
    }

    public static LocalCalendar[] findAll(Account account, ContentProviderClient providerClient) throws RemoteException {
        @Cleanup Cursor cursor = providerClient.query(calendarsURI(account),
                new String[] { CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME },
                CalendarContract.Calendars.DELETED + "=0 AND " + CalendarContract.Calendars.SYNC_EVENTS + "=1", null, null);

        LinkedList<LocalCalendar> calendars = new LinkedList<LocalCalendar>();
        while (cursor != null && cursor.moveToNext())
            calendars.add(new LocalCalendar(account, providerClient, cursor.getInt(0), cursor.getString(1)));
        return calendars.toArray(new LocalCalendar[0]);
    }

    public EncryptedLocalCalendar(Account account, ContentProviderClient providerClient, long id, String url) throws RemoteException {
        super(account, providerClient);
        this.id = id;
        this.url = url;
        sqlFilter = "ORIGINAL_ID IS NULL";
    }


	/* collection operations */

    @Override
    public String getCTag() throws LocalStorageException {
        try {
            @Cleanup Cursor c = providerClient.query(ContentUris.withAppendedId(calendarsURI(), id),
                    new String[] { COLLECTION_COLUMN_CTAG }, null, null, null);
            if (c.moveToFirst()) {
                return c.getString(0);
            } else
                throw new LocalStorageException("Couldn't query calendar CTag");
        } catch(RemoteException e) {
            throw new LocalStorageException(e);
        }
    }

    @Override
    public void setCTag(String cTag) throws LocalStorageException {
        ContentValues values = new ContentValues(1);
        values.put(COLLECTION_COLUMN_CTAG, cTag);
        try {
            providerClient.update(ContentUris.withAppendedId(calendarsURI(), id), values, null, null);
        } catch(RemoteException e) {
            throw new LocalStorageException(e);
        }
    }

    @Override
    public long[] findUpdated() throws LocalStorageException {
        // mark (recurring) events with changed/deleted exceptions as dirty
        String where = entryColumnID() + " IN (SELECT DISTINCT " + CalendarContract.Events.ORIGINAL_ID + " FROM events WHERE " +
                CalendarContract.Events.ORIGINAL_ID + " IS NOT NULL AND (" + CalendarContract.Events.DIRTY + "=1 OR " + CalendarContract.Events.DELETED + "=1))";
        ContentValues dirty = new ContentValues(1);
        dirty.put(CalendarContract.Events.DIRTY, 1);
        try {
            int rows = providerClient.update(entriesURI(), dirty, where, null);
            if (rows > 0)
                Log.d(TAG, rows + " event(s) marked as dirty because of dirty/deleted exceptions");
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't mark events with updated exceptions as dirty", e);
        }

        // new find and return updated (master) events
        return super.findUpdated();
    }


	/* create/update/delete */

    public EncryptedEvent newResource(long localID, String resourceName, String eTag) {
        return new EncryptedEvent(localID, resourceName, eTag);
    }

    public void deleteAllExceptRemoteNames(Resource[] remoteResources) {
        List<String> sqlFileNames = new LinkedList<>();
        for (Resource res : remoteResources)
            sqlFileNames.add(DatabaseUtils.sqlEscapeString(res.getName()));

        // delete master events
        String where = entryColumnParentID() + "=?";
        where += sqlFileNames.isEmpty() ?
                " AND " + entryColumnRemoteName() + " IS NOT NULL"  :   // don't retain anything (delete all)
                " AND " + entryColumnRemoteName() + " NOT IN (" + StringUtils.join(sqlFileNames, ",") + ")";    // retain by remote file name
        if (sqlFilter != null)
            where += " AND (" + sqlFilter + ")";
        pendingOperations.add(ContentProviderOperation.newDelete(entriesURI())
                .withSelection(where, new String[] { String.valueOf(id) })
                .build());

        // delete exceptions, too
        where = entryColumnParentID() + "=?";
        where += sqlFileNames.isEmpty() ?
                " AND " + CalendarContract.Events.ORIGINAL_SYNC_ID + " IS NOT NULL"  :   // don't retain anything (delete all)
                " AND " + CalendarContract.Events.ORIGINAL_SYNC_ID + " NOT IN (" + StringUtils.join(sqlFileNames, ",") + ")";    // retain by remote file name
        pendingOperations.add(ContentProviderOperation
                        .newDelete(entriesURI())
                        .withSelection(where, new String[]{String.valueOf(id)})
                        .withYieldAllowed(true)
                        .build()
        );
    }

    @Override
    public void delete(Resource resource) {
        super.delete(resource);

        // delete all exceptions of this event, too
        pendingOperations.add(ContentProviderOperation
                        .newDelete(entriesURI())
                        .withSelection(CalendarContract.Events.ORIGINAL_ID + "=?", new String[] { Long.toString(resource.getLocalID()) })
                        .build()
        );
    }

    @Override
    public void clearDirty(Resource resource) {
        super.clearDirty(resource);

        // clear dirty flag of all exceptions of this event
        pendingOperations.add(ContentProviderOperation
                        .newUpdate(entriesURI())
                        .withValue(CalendarContract.Events.DIRTY, 0)
                        .withSelection(CalendarContract.Events.ORIGINAL_ID + "=?", new String[]{Long.toString(resource.getLocalID())})
                        .build()
        );
    }


	/* methods for populating the data object from the content provider */

    @Override
    public void populate(Resource resource) throws LocalStorageException {
        EncryptedEvent event = (EncryptedEvent)resource;

        try {
            @Cleanup EntityIterator iterEvents = CalendarContract.EventsEntity.newEntityIterator(
                    providerClient.query(
                            syncAdapterURI(CalendarContract.EventsEntity.CONTENT_URI),
                            null, CalendarContract.Events._ID + "=" + event.getLocalID(),
                            null, null),
                    providerClient
            );
            while (iterEvents.hasNext()) {
                Entity e = iterEvents.next();

                ContentValues values = e.getEntityValues();
                populateEvent(event, values);

                List<Entity.NamedContentValues> subValues = e.getSubValues();
                for (Entity.NamedContentValues subValue : subValues) {
                    values = subValue.values;
                    if (CalendarContract.Attendees.CONTENT_URI.equals(subValue.uri))
                        populateAttendee(event, values);
                    if (CalendarContract.Reminders.CONTENT_URI.equals(subValue.uri))
                        populateReminder(event, values);
                }

                populateExceptions(event);
            }
        } catch (RemoteException ex) {
            throw new LocalStorageException("Couldn't process locally stored event", ex);
        }
    }

    protected void populateEvent(EncryptedEvent e, ContentValues values) {
        e.setUid(values.getAsString(entryColumnUID()));

        e.setSummary(values.getAsString(CalendarContract.Events.TITLE));
        e.setLocation(values.getAsString(CalendarContract.Events.EVENT_LOCATION));
        e.setDescription(values.getAsString(CalendarContract.Events.DESCRIPTION));

        boolean allDay = values.getAsBoolean(CalendarContract.Events.ALL_DAY);
        long tsStart = values.getAsLong(CalendarContract.Events.DTSTART);
        Long tsEnd = values.getAsLong(CalendarContract.Events.DTEND);
        String duration = values.getAsString(CalendarContract.Events.DURATION);

        String tzId = null;
        if (allDay) {
            e.setDtStart(tsStart, null);
            if (tsEnd == null) {
                Dur dur = new Dur(duration);
                java.util.Date dEnd = dur.getTime(new java.util.Date(tsStart));
                tsEnd = dEnd.getTime();
            }
            e.setDtEnd(tsEnd, null);

        } else {
            // use the start time zone for the end time, too
            // because apps like Samsung Planner allow the user to change "the" time zone but change the start time zone only
            tzId = values.getAsString(CalendarContract.Events.EVENT_TIMEZONE);
            e.setDtStart(tsStart, tzId);
            if (tsEnd != null)
                e.setDtEnd(tsEnd, tzId);
            else if (!StringUtils.isEmpty(duration))
                e.setDuration(new Duration(new Dur(duration)));
        }

        // recurrence
        try {
            String strRRule = values.getAsString(CalendarContract.Events.RRULE);
            if (!StringUtils.isEmpty(strRRule))
                e.setRrule(new RRule(strRRule));

            String strRDate = values.getAsString(CalendarContract.Events.RDATE);
            if (!StringUtils.isEmpty(strRDate)) {
                RDate rDate = new RDate();
                rDate.setValue(strRDate);
                e.setRdate(rDate);
            }

            String strExRule = values.getAsString(CalendarContract.Events.EXRULE);
            if (!StringUtils.isEmpty(strExRule)) {
                ExRule exRule = new ExRule();
                exRule.setValue(strExRule);
                e.setExrule(exRule);
            }

            String strExDate = values.getAsString(CalendarContract.Events.EXDATE);
            if (!StringUtils.isEmpty(strExDate)) {
                // ignored, see https://code.google.com/p/android/issues/detail?id=21426
                ExDate exDate = new ExDate();
                exDate.setValue(strExDate);
                e.setExdate(exDate);
            }
        } catch (ParseException ex) {
            Log.w(TAG, "Couldn't parse recurrence rules, ignoring", ex);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Invalid recurrence rules, ignoring", ex);
        }

        if (values.containsKey(CalendarContract.Events.ORIGINAL_INSTANCE_TIME)) {
            // this event is an exception of a recurring event
            long originalInstanceTime = values.getAsLong(CalendarContract.Events.ORIGINAL_INSTANCE_TIME);
            boolean originalAllDay = values.getAsBoolean(CalendarContract.Events.ORIGINAL_ALL_DAY);
            Date originalDate = originalAllDay ?
                    new Date(originalInstanceTime) :
                    new DateTime(originalInstanceTime);
            if (originalDate instanceof DateTime)
                ((DateTime)originalDate).setUtc(true);
            e.setRecurrenceId(new RecurrenceId(originalDate));
        }

        // status
        if (values.containsKey(CalendarContract.Events.STATUS))
            switch (values.getAsInteger(CalendarContract.Events.STATUS)) {
                case CalendarContract.Events.STATUS_CONFIRMED:
                    e.setStatus(Status.VEVENT_CONFIRMED);
                    break;
                case CalendarContract.Events.STATUS_TENTATIVE:
                    e.setStatus(Status.VEVENT_TENTATIVE);
                    break;
                case CalendarContract.Events.STATUS_CANCELED:
                    e.setStatus(Status.VEVENT_CANCELLED);
            }

        // availability
        e.setOpaque(values.getAsInteger(CalendarContract.Events.AVAILABILITY) != CalendarContract.Events.AVAILABILITY_FREE);

        // set ORGANIZER only when there are attendees
        if (values.getAsBoolean(CalendarContract.Events.HAS_ATTENDEE_DATA) && values.containsKey(CalendarContract.Events.ORGANIZER))
            try {
                e.setOrganizer(new Organizer(new URI("mailto", values.getAsString(CalendarContract.Events.ORGANIZER), null)));
            } catch (URISyntaxException ex) {
                Log.e(TAG, "Error when creating ORGANIZER URI, ignoring", ex);
            }

        // classification
        switch (values.getAsInteger(CalendarContract.Events.ACCESS_LEVEL)) {
            case CalendarContract.Events.ACCESS_CONFIDENTIAL:
            case CalendarContract.Events.ACCESS_PRIVATE:
                e.setForPublic(false);
                break;
            case CalendarContract.Events.ACCESS_PUBLIC:
                e.setForPublic(true);
        }
    }

    void populateExceptions(EncryptedEvent e) throws RemoteException {
        @Cleanup Cursor c = providerClient.query(syncAdapterURI(CalendarContract.Events.CONTENT_URI),
                new String[]{CalendarContract.Events._ID, entryColumnRemoteName()},
                CalendarContract.Events.ORIGINAL_ID + "=?", new String[]{ String.valueOf(e.getLocalID()) }, null);
        while (c != null && c.moveToNext()) {
            long exceptionId = c.getLong(0);
            String exceptionRemoteName = c.getString(1);
            try {
                EncryptedEvent exception = new EncryptedEvent(exceptionId, exceptionRemoteName, null);
                populate(exception);
                e.getExceptions().add(exception);
            } catch (LocalStorageException ex) {
                Log.e(TAG, "Couldn't find exception details, ignoring");
            }
        }
    }

    void populateAttendee(EncryptedEvent event, ContentValues values) throws RemoteException {
        try {
            Attendee attendee = new Attendee(new URI("mailto", values.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL), null));
            ParameterList params = attendee.getParameters();

            String cn = values.getAsString(CalendarContract.Attendees.ATTENDEE_NAME);
            if (cn != null)
                params.add(new Cn(cn));

            // type
            int type = values.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE);
            params.add((type == CalendarContract.Attendees.TYPE_RESOURCE) ? CuType.RESOURCE : CuType.INDIVIDUAL);

            // role
            int relationship = values.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP);
            switch (relationship) {
                case CalendarContract.Attendees.RELATIONSHIP_ORGANIZER:
                    params.add(Role.CHAIR);
                    break;
                case CalendarContract.Attendees.RELATIONSHIP_ATTENDEE:
                case CalendarContract.Attendees.RELATIONSHIP_PERFORMER:
                case CalendarContract.Attendees.RELATIONSHIP_SPEAKER:
                    params.add((type == CalendarContract.Attendees.TYPE_REQUIRED) ? Role.REQ_PARTICIPANT : Role.OPT_PARTICIPANT);
                    break;
                case CalendarContract.Attendees.RELATIONSHIP_NONE:
                    params.add(Role.NON_PARTICIPANT);
            }

            // status
            switch (values.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS)) {
                case CalendarContract.Attendees.ATTENDEE_STATUS_INVITED:
                    params.add(PartStat.NEEDS_ACTION);
                    break;
                case CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED:
                    params.add(PartStat.ACCEPTED);
                    break;
                case CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED:
                    params.add(PartStat.DECLINED);
                    break;
                case CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE:
                    params.add(PartStat.TENTATIVE);
                    break;
            }

            event.getAttendees().add(attendee);
        } catch (URISyntaxException ex) {
            Log.e(TAG, "Couldn't parse attendee information, ignoring", ex);
        }
    }

    void populateReminder(EncryptedEvent event, ContentValues row) throws RemoteException {
        VAlarm alarm = new VAlarm(new Dur(0, 0, -row.getAsInteger(CalendarContract.Reminders.MINUTES), 0));

        PropertyList props = alarm.getProperties();
        props.add(Action.DISPLAY);
        props.add(new Description(event.getSummary()));
        event.getAlarms().add(alarm);
    }


	/* content builder methods */

    @Override
    protected ContentProviderOperation.Builder buildEntry(ContentProviderOperation.Builder builder, Resource resource) {
        EncryptedEvent event = (EncryptedEvent)resource;

        builder = builder
                .withValue(CalendarContract.Events.CALENDAR_ID, id)
                .withValue(CalendarContract.Events.ALL_DAY, event.isAllDay() ? 1 : 0)
                .withValue(CalendarContract.Events.DTSTART, event.getDtStartInMillis())
                .withValue(CalendarContract.Events.EVENT_TIMEZONE, event.getDtStartTzID())
                .withValue(CalendarContract.Events.HAS_ALARM, event.getAlarms().isEmpty() ? 0 : 1)
                .withValue(CalendarContract.Events.HAS_ATTENDEE_DATA, event.getAttendees().isEmpty() ? 0 : 1)
                .withValue(CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS, 1)
                .withValue(CalendarContract.Events.GUESTS_CAN_MODIFY, 1)
                .withValue(CalendarContract.Events.GUESTS_CAN_SEE_GUESTS, 1);

        RecurrenceId recurrenceId = event.getRecurrenceId();
        if (recurrenceId == null) {
            // this event is a "master event" (not an exception)
            builder = builder
                    .withValue(entryColumnRemoteName(), event.getName())
                    .withValue(entryColumnETag(), event.getETag())
                    .withValue(entryColumnUID(), event.getUid());
        } else {
            builder = builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, event.getName());

            // ORIGINAL_INSTANCE_TIME and ORIGINAL_ALL_DAY is set in buildExceptions.
            // It's not possible to use only the RECURRENCE-ID to calculate
            // ORIGINAL_INSTANCE_TIME and ORIGINAL_ALL_DAY because iCloud sends DATE-TIME
            // RECURRENCE-IDs even if the original event is an all-day event.
        }

        boolean recurring = false;
        if (event.getRrule() != null) {
            recurring = true;
            builder = builder.withValue(CalendarContract.Events.RRULE, event.getRrule().getValue());
        }
        if (event.getRdate() != null) {
            recurring = true;
            RDate rDate = event.getRdate();
            String rDateStr = rDate.getValue();
            if (rDate.getTimeZone() != null)
                rDateStr = DateUtils.findAndroidTimezoneID(rDate.getTimeZone().getID()) + ";" + rDateStr;
            builder = builder.withValue(CalendarContract.Events.RDATE, rDateStr);
        }
        if (event.getExrule() != null)
            builder = builder.withValue(CalendarContract.Events.EXRULE, event.getExrule().getValue());
        if (event.getExdate() != null) {
            ExDate exDate = event.getExdate();
            String exDateStr = exDate.getValue();
            if (exDate.getTimeZone() != null)
                exDateStr = DateUtils.findAndroidTimezoneID(exDate.getTimeZone().getID()) + ";" + exDateStr;
            builder = builder.withValue(CalendarContract.Events.EXDATE, exDateStr);
        }

        // set either DTEND for single-time events or DURATION for recurring events
        // because that's the way Android likes it (see docs)
        if (recurring) {
            // calculate DURATION from start and end date
            Duration duration = new Duration(event.getDtStart().getDate(), event.getDtEnd().getDate());
            builder = builder.withValue(CalendarContract.Events.DURATION, duration.getValue());
        } else {
            builder = builder
                    .withValue(CalendarContract.Events.DTEND, event.getDtEndInMillis())
                    .withValue(CalendarContract.Events.EVENT_END_TIMEZONE, event.getDtEndTzID());
        }

        if (event.getSummary() != null)
            builder = builder.withValue(CalendarContract.Events.TITLE, event.getSummary());
        if (event.getLocation() != null)
            builder = builder.withValue(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
        if (event.getDescription() != null)
            builder = builder.withValue(CalendarContract.Events.DESCRIPTION, event.getDescription());

        if (event.getOrganizer() != null && event.getOrganizer().getCalAddress() != null) {
            URI organizer = event.getOrganizer().getCalAddress();
            if (organizer.getScheme() != null && organizer.getScheme().equalsIgnoreCase("mailto"))
                builder = builder.withValue(CalendarContract.Events.ORGANIZER, organizer.getSchemeSpecificPart());
        }

        Status status = event.getStatus();
        if (status != null) {
            int statusCode = CalendarContract.Events.STATUS_TENTATIVE;
            if (status == Status.VEVENT_CONFIRMED)
                statusCode = CalendarContract.Events.STATUS_CONFIRMED;
            else if (status == Status.VEVENT_CANCELLED)
                statusCode = CalendarContract.Events.STATUS_CANCELED;
            builder = builder.withValue(CalendarContract.Events.STATUS, statusCode);
        }

        builder = builder.withValue(CalendarContract.Events.AVAILABILITY, event.isOpaque() ? CalendarContract.Events.AVAILABILITY_BUSY : CalendarContract.Events.AVAILABILITY_FREE);

        if (event.getForPublic() != null)
            builder = builder.withValue(CalendarContract.Events.ACCESS_LEVEL, event.getForPublic() ? CalendarContract.Events.ACCESS_PUBLIC : CalendarContract.Events.ACCESS_PRIVATE);

        return builder;
    }


    @Override
    protected void addDataRows(Resource resource, long localID, int backrefIdx) {
        EncryptedEvent event = (EncryptedEvent)resource;
        // add exceptions
        // TODO - support exceptions
        //for (EncryptedEvent exception : event.getExceptions())
        //    pendingOperations.add(buildException(newDataInsertBuilder(CalendarContract.Events.CONTENT_URI, CalendarContract.Events.ORIGINAL_ID, localID, backrefIdx), event, exception).build());
        
        // add attendees
        for (Attendee attendee : event.getAttendees())
            pendingOperations.add(buildAttendee(newDataInsertBuilder(CalendarContract.Attendees.CONTENT_URI, CalendarContract.Attendees.EVENT_ID, localID, backrefIdx), attendee).build());
        // add reminders
        for (VAlarm alarm : event.getAlarms())
            pendingOperations.add(buildReminder(newDataInsertBuilder(CalendarContract.Reminders.CONTENT_URI, CalendarContract.Reminders.EVENT_ID, localID, backrefIdx), alarm).build());
    }

    @Override
    protected void removeDataRows(Resource resource) {
        EncryptedEvent event = (EncryptedEvent)resource;
        // delete exceptions
        pendingOperations.add(ContentProviderOperation.newDelete(syncAdapterURI(CalendarContract.Events.CONTENT_URI))
                .withSelection(CalendarContract.Events.ORIGINAL_ID + "=?", new String[] { String.valueOf(event.getLocalID())}).build());
        // delete attendees
        pendingOperations.add(ContentProviderOperation.newDelete(syncAdapterURI(CalendarContract.Attendees.CONTENT_URI))
                .withSelection(CalendarContract.Attendees.EVENT_ID + "=?", new String[] { String.valueOf(event.getLocalID()) }).build());
        // delete reminders
        pendingOperations.add(ContentProviderOperation.newDelete(syncAdapterURI(CalendarContract.Reminders.CONTENT_URI))
                .withSelection(CalendarContract.Reminders.EVENT_ID + "=?", new String[] { String.valueOf(event.getLocalID()) }).build());
    }


    protected ContentProviderOperation.Builder buildException(ContentProviderOperation.Builder builder, EncryptedEvent master, EncryptedEvent exception) {
        buildEntry(builder, exception);
        builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, exception.getName());

        // Some servers (iCloud, for instance) return RECURRENCE-ID with DATE-TIME even if
        // the original event is an all-day event. Workaround: determine value of ORIGINAL_ALL_DAY
        // by original event type (all-day or not) and not by whether RECURRENCE-ID is DATE or DATE-TIME.

        RecurrenceId recurrenceId = exception.getRecurrenceId();
        Date date = recurrenceId.getDate();

        boolean originalAllDay = master.isAllDay();
        if (originalAllDay && date instanceof DateTime) {
            String value = recurrenceId.getValue();
            if (value.matches("^\\d{8}T\\d{6}$"))
                try {
                    // no "Z" at the end indicates "local" time
                    // so this is a "local" time, but it should be a ical4j Date without time
                    date = new Date(value.substring(0, 8));
                } catch (ParseException e) {
                    Log.e(TAG, "Couldn't parse DATE part of DATE-TIME RECURRENCE-ID", e);
                }
        }

        builder.withValue(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, date.getTime());
        builder.withValue(CalendarContract.Events.ORIGINAL_ALL_DAY, originalAllDay ? 1 : 0);
        return builder;
    }

    @SuppressLint("InlinedApi")
    protected ContentProviderOperation.Builder buildAttendee(ContentProviderOperation.Builder builder, Attendee attendee) {
        Uri member = Uri.parse(attendee.getValue());
        String email = member.getSchemeSpecificPart();

        Cn cn = (Cn)attendee.getParameter(Parameter.CN);
        if (cn != null)
            builder = builder.withValue(CalendarContract.Attendees.ATTENDEE_NAME, cn.getValue());

        int type = CalendarContract.Attendees.TYPE_NONE;

        CuType cutype = (CuType)attendee.getParameter(Parameter.CUTYPE);
        if (cutype == CuType.RESOURCE)
            type = CalendarContract.Attendees.TYPE_RESOURCE;
        else {
            Role role = (Role)attendee.getParameter(Parameter.ROLE);
            int relationship;
            if (role == Role.CHAIR)
                relationship = CalendarContract.Attendees.RELATIONSHIP_ORGANIZER;
            else {
                relationship = CalendarContract.Attendees.RELATIONSHIP_ATTENDEE;
                if (role == Role.OPT_PARTICIPANT)
                    type = CalendarContract.Attendees.TYPE_OPTIONAL;
                else if (role == Role.REQ_PARTICIPANT)
                    type = CalendarContract.Attendees.TYPE_REQUIRED;
            }
            builder = builder.withValue(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, relationship);
        }

        int status = CalendarContract.Attendees.ATTENDEE_STATUS_NONE;
        PartStat partStat = (PartStat)attendee.getParameter(Parameter.PARTSTAT);
        if (partStat == null || partStat == PartStat.NEEDS_ACTION)
            status = CalendarContract.Attendees.ATTENDEE_STATUS_INVITED;
        else if (partStat == PartStat.ACCEPTED)
            status = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED;
        else if (partStat == PartStat.DECLINED)
            status = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;
        else if (partStat == PartStat.TENTATIVE)
            status = CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE;

        return builder
                .withValue(CalendarContract.Attendees.ATTENDEE_EMAIL, email)
                .withValue(CalendarContract.Attendees.ATTENDEE_TYPE, type)
                .withValue(CalendarContract.Attendees.ATTENDEE_STATUS, status);
    }

    protected ContentProviderOperation.Builder buildReminder(ContentProviderOperation.Builder builder, VAlarm alarm) {
        int minutes = 0;

        if (alarm.getTrigger() != null) {
            Dur duration = alarm.getTrigger().getDuration();
            if (duration != null) {
                // negative value in TRIGGER means positive value in Reminders.MINUTES and vice versa
                minutes = -(((duration.getWeeks() * 7 + duration.getDays()) * 24 + duration.getHours()) * 60 + duration.getMinutes());
                if (duration.isNegative())
                    minutes *= -1;
            }
        }

        Log.d(TAG, "Adding alarm " + minutes + " minutes before");

        return builder
                .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                .withValue(CalendarContract.Reminders.MINUTES, minutes);
    }



	/* private helper methods */

    protected static Uri calendarsURI(Account account) {
        return CalendarContract.Calendars.CONTENT_URI.buildUpon().appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    protected Uri calendarsURI() {
        return calendarsURI(account);
    }

}
