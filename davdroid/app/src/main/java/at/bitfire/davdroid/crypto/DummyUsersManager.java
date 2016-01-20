package at.bitfire.davdroid.crypto;

import java.util.HashMap;

/**
 * A dummy implementation of UsersManager.
 * Uses hard-coded users data (userID + secret).
 */
public class DummyUsersManager implements UsersManager {

    private HashMap<String, String> usersData;

    public DummyUsersManager() {
        usersData.put("xcfdxcfd", "VeryStrongPassword");
    }

    @Override
    public String getSecret(String userID) {
        return usersData.get(userID);
    }

    @Override
    public boolean userExists(String userID) {
        return usersData.containsKey(userID);
    }
}
