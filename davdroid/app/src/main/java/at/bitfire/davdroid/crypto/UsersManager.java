package at.bitfire.davdroid.crypto;

/**
 * Used for managing information regarding the users of the application.
 */
public interface UsersManager {

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
     * @return true if userID exists
     */
    public void removeUser(String userID);


    /**
     * Checks weather userID is known by the UsersManager.
     * @param userID the userID to check
     * @return true if userID exists
     */
    public boolean userExists(String userID);
}
