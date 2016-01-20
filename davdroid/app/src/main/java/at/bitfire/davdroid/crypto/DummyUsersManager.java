package at.bitfire.davdroid.crypto;

import java.util.HashMap;
import java.util.Map;

/**
 * A dummy implementation of UsersManager.
 * Uses hard-coded users data (userID + secret).
 */
public class DummyUsersManager implements UsersManager {

    private Map<String, String> usersData;

    public DummyUsersManager() {
        usersData = new HashMap<>();
        usersData.put("xcfdxcfd-5", "VeryStrongPassword");
    }

    @Override
    public String getSecret(String userID) {
        //return usersData.get(userID);
        //TODO change this to read from userData
        return "VeryStrongPassword";
    }

    @Override
    public boolean userExists(String userID) {
        return usersData.containsKey(userID);
    }
}
