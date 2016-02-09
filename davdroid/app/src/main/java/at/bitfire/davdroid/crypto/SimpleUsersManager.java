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
        protected byte[] pbKey;
        protected byte[] encSK;
        protected byte[] signature;


//        public UserState(String secret, boolean state, boolean toRemove) {
//            this.secret = secret;
//            this.state = state;
//            this.toRemove = toRemove;
//            this.pbKey = null;
//            this.encSK = null;
//            this.signature = null;
//        }

        public UserState(String secret, boolean state, boolean toRemove, byte[] pbKey, byte[] encSK, byte[] signature) {
            this.secret = secret;
            this.state = state;
            this.toRemove = toRemove;
            this.pbKey = pbKey;
            this.encSK = encSK;
            this.signature = signature;
        }
    }

    public SimpleUsersManager() {
        usersDataSuperSet = new HashMap<>();
        final String USER1 = "xcfdxcfd";
        final String USER2 = "shmir";
        usersDataSuperSet.put(USER1, new UserState("VeryStrongPassword",false,false,null,null,null));
        usersDataSuperSet.put(USER2, new UserState("VeryStrongPassword",false,false,null,null,null));

        usersData = new HashMap<>();
        //usersData.put(USER1, usersDataSuperSet.get(USER1));
        //usersData.put(USER2, usersDataSuperSet.get(USER2));
        needsRemoval = false;
    }

    @Override
    public String getSecret(String userID) {
        return usersDataSuperSet.get(userID).secret;
    }

//    @Override
//    public void addUser(String user) {
//        if (usersDataSuperSet.containsKey(user))
//            usersData.put(user,usersDataSuperSet.get(user));
//    }

    public void addUser(String userID,byte[] pbKey, byte[] encSK, byte[] signature) {
        if (usersDataSuperSet.containsKey(userID)) {
            if (usersData.containsKey(userID)) {
                usersData.put(userID, new UserState(usersDataSuperSet.get(userID).secret,
                        usersData.get(userID).state,
                        usersData.get(userID).toRemove,
                        pbKey,
                        encSK,
                        signature));

            } else {
                usersData.put(userID, new UserState(usersDataSuperSet.get(userID).secret,
                        false,
                        false,
                        pbKey,
                        encSK,
                        signature));
            }
        }
    }
    @Override
    public void authUser(String user) {
        if (usersData.containsKey(user))
            usersData.put(user,new UserState(usersData.get(user).secret,
                                             true,
                                             false,
                                             usersData.get(user).pbKey,
                                             usersData.get(user).encSK,
                                             usersData.get(user).signature));
    }

    @Override
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
    public Set<String> getValidUsers() {
        Set<String> usersSet = usersData.keySet();
        for (String userID : usersData.keySet()) {
            if (userShouldBeRemoved(userID))
                usersSet.remove(userID);
        }
        return usersSet;
    }

    @Override
    public boolean needsRemoval() {
        return this.needsRemoval;
    }

    @Override
    public void usersRemoved() {
        for (String userID : this.getUsers()) {
            if (this.userShouldBeRemoved(userID)) {
                this.removeUser(userID);
            }
        }
        this.needsRemoval = false;
    }

    public boolean userShouldBeRemoved(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).toRemove;
        return false;
    }

    public byte[] getSK(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).encSK;
        return null;
    }
    public byte[] getPbKey(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).pbKey;
        return null;
    }

    public byte[] getSignature(String userID) {
        if (usersData.containsKey(userID))
            return usersData.get(userID).signature;
        return null;
    }


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
