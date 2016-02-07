package at.bitfire.davdroid.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A dummy implementation of UsersManager.
 * Uses hard-coded users data (userID + secret).
 */
public class DummyUsersManager implements UsersManager {

    private Map<String, String> usersData;
    private Map<String, String> usersDataSuperSet;

    public DummyUsersManager() {
        usersDataSuperSet = new HashMap<>();
        usersDataSuperSet.put("xcfdxcfd", "VeryStrongPassword");
        usersDataSuperSet.put("shmir", "VeryStrongPassword");

        usersData = new HashMap<>();
        usersData.put("xcfdxcfd", "VeryStrongPassword");
//        usersData.put("shmir", "VeryStrongPassword");
    }

    @Override
    public String getSecret(String userID) {
        return usersData.get(userID);
    }

    public void addUser(String user) {
        if (usersDataSuperSet.containsKey(user))
            usersData.put(user,usersDataSuperSet.get(user));
    }

    public void removeUser(String user) {
        if (usersData.containsKey(user))
            usersData.remove(user);
    }

    @Override
    public boolean userExists(String userID) {
        return usersData.containsKey(userID);
    }

    @Override
    public Set<String> getUsers() {
        return usersDataSuperSet.keySet();
    }
}
