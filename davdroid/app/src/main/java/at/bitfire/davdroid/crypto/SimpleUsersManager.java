package at.bitfire.davdroid.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A dummy implementation of UsersManager.
 * Uses hard-coded users data (userID + secret).
 */
public class SimpleUsersManager implements UsersManager {

    private Map<String, UserState> usersData;
    private Map<String, UserState> usersDataSuperSet;
    private boolean needsRemoval;
    public class UserState {

        String secret;
        boolean state;
        boolean toRemove;

        public UserState(String secret, boolean state, boolean toRemove) {
            this.secret = secret;
            this.state = state;
            this.toRemove = toRemove;
        }
    }

    public SimpleUsersManager() {
        usersDataSuperSet = new HashMap<>();
        final String USER1 = "xcfdxcfd";
        final String USER2 = "shmir";
        usersDataSuperSet.put(USER1, new UserState("VeryStrongPassword",false,false));
        usersDataSuperSet.put(USER2, new UserState("VeryStrongPassword",false,false));

        usersData = new HashMap<>();
        usersData.put(USER1, usersDataSuperSet.get(USER1));
        usersData.put(USER2, usersDataSuperSet.get(USER2));
        needsRemoval = false;
    }

    @Override
    public String getSecret(String userID) {
        return usersDataSuperSet.get(userID).secret;
    }

    @Override
    public void addUser(String user) {
        if (usersDataSuperSet.containsKey(user))
            usersData.put(user,usersDataSuperSet.get(user));
    }

    @Override
    public void authUser(String user) {
        if (usersDataSuperSet.containsKey(user))
            usersData.put(user,new UserState(usersDataSuperSet.get(user).secret,true,false));
    }

    @Override
    public void markToRemoveUser(String user) {
        if (usersData.containsKey(user))
            usersData.put(user,new UserState(usersDataSuperSet.get(user).secret,false,true));
        needsRemoval = true;
    }

    @Override
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
        return usersData.keySet();
    }

    @Override
    public boolean needsRemoval() {
        return this.needsRemoval;
    }

    @Override
    public void usersRemoved() {
        this.needsRemoval = false;
    }

    public boolean userShouldBeRemoved(String userID) {
        if (usersData.containsKey(userID))
            usersData.get(userID);
        needsRemoval = true;
    }
}
