package at.bitfire.davdroid.crypto;

import java.util.Set;

/**
 * Used for managing information regarding the users of the application.
 */
public interface UsersManager {

    protected class KeyRecord {

        public KeyRecord(byte[] pbKey, byte[] encSK, byte[] signature) {
            this.pbKey = pbKey;
            this.encSK = encSK;
            this.signature = signature;
        }

        protected byte[] pbKey;
        protected byte[] encSK;
        protected byte[] signature;
    }

    /**
     * A secret is a security parameter that is known for all the users of the application.
     * We assume that the secrets of all the user is synchronized outside the calendar (i.e using
     * a secured server during the registration).
     * The secret is used to validate the user's data.
     * @param userID the common known ID of the user
     * @return the secret of userID
     */
    public String getSecret(String userID);

    /**
     * add a user if exists in superset
     * @param userID the userID to check
     * @return true if userID exists
     */
    public void addUser(String userID);

    /**
     * remove a user
     * @param userID the userID to check
     */
    public void removeUser(String userID);

    /**
     * Mark user for removal
     * @param userID the userID to mark
     */
    public void  markToRemoveUser(String userID);

    /**
     * Authorize user
     * @param userID the userID to authorize
     */
    public void authUser(String userID);

    /**
     * Checks weather userID is known by the UsersManager.
     * @param userID the userID to check
     * @return true if userID exists
     */
    public boolean userExists(String userID);

    /**
     * Checks whether there are users that need to be removed.
     * @return true users need to be removed
     */
    public boolean needsRemoval();


    /**
     * Mark all users requiring removal as removed
     * @return true users need to be removed
     */
    public void usersRemoved();

    /**
     * Checks weather userID is known by the UsersManager.
     * @param userID the userID to check
     * @return true if userID exists
     */
    public Set<String> getUsers();


    public boolean userShouldBeRemoved(String userID);


}
