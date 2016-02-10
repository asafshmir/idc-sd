package at.bitfire.davdroid.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Used for managing information regarding the users of the application.
 */
public class UsersManager  {

    // A Map indicating all of the users sharing the calendar
    private Map<String, UserState> usersData;

    // A Map holding the "Secret Key" synchronized offline by all users who
    // could be getting access to the calendar
    private Map<String, String> usersDataSuperSet;

    // A boolean indicating whether any users has been marked for removal
    private boolean needsRemoval;


    public class UserState {

        String secret;
        boolean state;
        boolean toRemove;
        protected byte[] pbKey;
        protected byte[] encSK;
        protected byte[] signature;

        public UserState(String secret, boolean state, boolean toRemove, byte[] pbKey, byte[] encSK, byte[] signature) {
            this.secret = secret;
            this.state = state;
            this.toRemove = toRemove;
            this.pbKey = pbKey;
            this.encSK = encSK;
            this.signature = signature;
        }
    }

    public UsersManager() {
        usersDataSuperSet = new HashMap<>();
        final String USER1 = "xcfdxcfd";
        final String USER2 = "shmir";
        usersDataSuperSet.put(USER1, "VeryStrongPassword");
        usersDataSuperSet.put(USER2, "VeryStrongPassword");

        usersData = new HashMap<>();
        needsRemoval = false;
    }


    /**
     * A secret is a security parameter that is known for all the users of the application.
     * We assume that the secrets of all the user is synchronized outside the calendar (i.e using
     * a secured server during the registration).
     * The secret is used to validate the user's data.
     *  Uses hard-coded users data (userID + secret).
     * @param userID the common known ID of the user
     * @return the secret of userID
     */

    public String getSecret(String userID) {
        return usersDataSuperSet.get(userID);
    }

    /**
     * add a user if exists in superset
     * @param userID the userID to check
     * @param pbKey the User's public Key
     * @param encSK the User's symmetric key
     * @param signature the User's signature of the public key
     * @return true if userID exists
     */
    public void addUser(String userID,byte[] pbKey, byte[] encSK, byte[] signature) {
        if (usersDataSuperSet.containsKey(userID)) {
            if (usersData.containsKey(userID)) {
                usersData.put(userID, new UserState(usersDataSuperSet.get(userID),
                        usersData.get(userID).state,
                        usersData.get(userID).toRemove,
                        pbKey,
                        encSK,
                        signature));

            } else {
                usersData.put(userID, new UserState(usersDataSuperSet.get(userID),
                        false,
                        false,
                        pbKey,
                        encSK,
                        signature));
            }
        }
    }

    /**
     * Authorize user
     * @param userID the userID to authorize
     */
    public void authUser(String user) {
        if (usersData.containsKey(user))
            usersData.put(user,new UserState(usersData.get(user).secret,
                                             true,
                                             false,
                                             usersData.get(user).pbKey,
                                             usersData.get(user).encSK,
                                             usersData.get(user).signature));
    }

    /**
     * Mark user for removal
     * @param userID the userID to mark for removal
     */
    public void markToRemoveUser(String user) {
        if (usersData.containsKey(user))
            usersData.put(user, new UserState(usersData.get(user).secret,
                                              false,
                                              true,
                                              usersData.get(user).pbKey,
                                              usersData.get(user).encSK,
                                              usersData.get(user).signature));
        needsRemoval = true;
    }


    /**
     * Mark user to keep
     * @param userID the userID to mark for keeping
     */
    public void markToKeepUser(String user) {
        if (usersData.containsKey(user))
            usersData.put(user, new UserState(usersData.get(user).secret,
                    false,
                    false,
                    usersData.get(user).pbKey,
                    usersData.get(user).encSK,
                    usersData.get(user).signature));

        needsRemoval = false;
        for (String userID : getUsers()) {
            if (userShouldBeRemoved(userID))
                needsRemoval = true;
        }

    }

    /**
     * remove a user
     * @param userID the userID to remove
     */
    public void removeUser(String user) {
        if (usersData.containsKey(user))
            usersData.remove(user);
    }


    /**
     * Checks weather userID is known by the UsersManager.
     * @param userID the userID to check
     * @return true if userID exists
     */
    public boolean userExists(String userID) {
        return usersData.containsKey(userID);
    }


    /**
     * Returns all users, including users marked for removal
     */
    public Set<String> getUsers() {
        return usersData.keySet();
    }

    /**
     * Returns only valid users, without users marked for removal
     */
    public Set<String> getValidUsers() {
        Set<String> usersSet = usersData.keySet();
        for (String userID : usersData.keySet()) {
            if (userShouldBeRemoved(userID))
                usersSet.remove(userID);
        }
        return usersSet;
    }

    /**
     * Checks whether there are users that need to be removed.
     * @return true users need to be removed
     */
    public boolean needsRemoval() {
        return this.needsRemoval;
    }

    /**
     * Mark all users requiring removal as removed
     * @return true users need to be removed
     */
    public void usersRemoved() {
        for (String userID : this.getUsers()) {
            if (this.userShouldBeRemoved(userID)) {
                this.removeUser(userID);
            }
        }
        this.needsRemoval = false;
    }

    /**
     * Check if a specific user should be removed
     * @param userID the UserID to check
     * @return boolean indicating whether a user should be removed
     */
    public boolean userShouldBeRemoved(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).toRemove;
        return false;
    }

    /**
     * Get the Symmetric Key for a user
     * @param userID the UserID to look for his key
     * @return the Symmetric Key for a user
     */
    public byte[] getEncSK(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).encSK;
        return null;
    }

    /**
     * Get the Public Key for a user
     * @param userID the UserID to look for his key
     * @return the Public Key for a user
     */
    public byte[] getPbKey(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).pbKey;
        return null;
    }

    /**
     * Get the Public Key's signature for a user
     * @param userID the UserID to look for his key
     * @return the Public Key's signature for a user
     */
    public byte[] getSignature(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).signature;
        return null;
    }

    /**
     * Update the SK for the userID
     * @param userID the UserID to look for updating his key
     * @return whether the Symmetric Key update was successful
     */
    public boolean updateSK(String userID, byte[] sk) {
        if (usersData.containsKey(userID)) {
            usersData.put(userID,
                    new UserState(usersData.get(userID).secret,
                            usersData.get(userID).state,
                            usersData.get(userID).toRemove,
                            usersData.get(userID).pbKey,
                            sk,
                            usersData.get(userID).signature));
            return true;
        }
        return false;
    }

}
