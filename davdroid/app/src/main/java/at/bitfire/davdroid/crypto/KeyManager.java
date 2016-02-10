package at.bitfire.davdroid.crypto;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;

import at.bitfire.davdroid.resource.Event;
import lombok.Getter;

public class KeyManager {

    private static final String TAG = "davdroid.KeyManager";
    //TODO move constants to SyncManager
    public static final String KEY_STORAGE_EVENT_NAME = "KeyManagerPleaseWork";
    public  static final String EVENT_TIME_FORMAT = "dd-MM-yyyy hh:mm:ss";
    public  static final String KEY_STORAGE_EVENT_TIME = "11-02-2016 02:00:00";
    public  static final String KEY_STORAGE_EVENT_TIME_END = "11-02-2016 23:00:00";
    //TODO move to SyncManager
    public static boolean isKeyManagerEvent(Event e) {
        return e.summary.equals(KEY_STORAGE_EVENT_NAME);
    }

    // Singleton instance
    private static KeyManager instance = null;

    // A map from userID to KeyRecord
    protected String userID;

//    protected ArrayList<String> usersToRemove;
//    protected boolean usersRemoved;

    // TODO support keyBank per account-name
    // TODO add setActiveAccount to support multiple accounts
    protected UsersManager usersManager;

    @Getter private boolean updated;

    // Asymmetric key-pair
    protected KeyPair asymKeyPair;

    // JSON tag names and attributes
    private final static String KEYBANK_TAG = "key-bank";
    private final static String USER_ID_ATTR = "user-id";
    private final static String PUBLIC_KEY_ATTR = "public-key";
    private final static String ENC_SK_ATTR = "enc-sk";
    private final static String SIGNATURE_ATTR = "signature";

    private final static String SKLIST_TAG = "sk-list";
    private final static String PUBLIC_KEY_PREFIX_ATTR = "public-key-prefix";

    private final static String KEYPAIR_TAG = "key-pair";
    private final static String PRIVATE_KEY_ATTR = "private-key";

    private final static int PUBLIC_KEY_PREFIX_SIZE = 64;

    private KeyManager() {
        usersManager = new SimpleUsersManager();
        updated = false;
    }


    public static KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }

    public String syncAsymKeyPair(String keyPairData) {
        if (keyPairData == null) {
            Log.i(TAG, "Got an empty data, generating KeyPair");
            asymKeyPair = CryptoUtils.generateRandomKeyPair();
        } else {
            Log.i(TAG, "Got a KeyPair");
            asymKeyPair = stringToKeyPair(keyPairData);
        }
        return keyPairToString(asymKeyPair);
    }

    private KeyPair stringToKeyPair(String data) {
        Log.i(TAG, "Converting a string to KeyPair");
        KeyPair keyPair;

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONObject keyPairObj = rootObj.optJSONObject(KEYPAIR_TAG);
            final byte[] pbKeyData = Base64.decode(keyPairObj.optString(PUBLIC_KEY_ATTR), Base64.DEFAULT);
            final byte[] prKeyData = Base64.decode(keyPairObj.optString(PRIVATE_KEY_ATTR), Base64.DEFAULT);

            PublicKey pbKey;
            pbKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM,"SC").generatePublic(new X509EncodedKeySpec(pbKeyData));
            PrivateKey prKey;
            prKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM,"SC").generatePrivate(new PKCS8EncodedKeySpec(prKeyData));

            keyPair = new KeyPair(pbKey, prKey);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

        return keyPair;
    }

    private String keyPairToString(KeyPair keyPair) {
        Log.i(TAG, "Converting a KeyPair to string");
        if (keyPair == null) {
            Log.e(TAG, "Got a null keyPair");
            return null;
        }
        JSONObject rootObj = new JSONObject();

        try {
            JSONObject keyPairObj = new JSONObject();
            keyPairObj.put(PUBLIC_KEY_ATTR, Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT));
            keyPairObj.put(PRIVATE_KEY_ATTR, Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT));
            rootObj.put(KEYPAIR_TAG, keyPairObj);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return rootObj.toString();
    }

    // TODO handle multiple accounts per KeyManager
    public String initKeyBank(String userID, String keyBankData) {
        updated = false;
        this.userID = userID;
        byte[] pbkey = asymKeyPair.getPublic().getEncoded();

        // KeyBank is empty, create the first record
        if (keyBankData == null) {
            Log.i(TAG, "Got an empty KeyBank data, generating random symmetric key");
            byte[] sk = CryptoUtils.generateRandomSymmetricKey();
            Log.i(TAG, "SK Generated: " + sk.toString());
            Log.i(TAG, "Adding first user: " + this.userID + " to the KeyBank");
            addKeyRecord(usersManager, userID, pbkey, sk);
            updated = true;
        } else {
            Log.i(TAG, "Got a KeyBank data, parse it");
            readUsersFromKeyBank(keyBankData);

            // userID is not in yet in the KeyBank - add a KeyRecord for him
            // or the sk is null because this user is not validated yet
            if (!usersManager.userExists(userID) ||
                (usersManager.userExists(userID) && (getSK() == null))) {
                Log.i(TAG, "User: " + this.userID + " doesn't exist in KeyBank, add it");
                addKeyRecord(usersManager,userID, pbkey, null);

                updated = true;
            }
        }

        // Try to validate other users
        boolean validate = validateAllUsers();
        updated = updated || validate;

        // Return the new key-bank
        return keyBankToString();
    }

    private void addKeyRecord(UsersManager kb, String userID, byte[] pbKey, byte[] sk) {
        Log.i(TAG, "Adding KeyRecord to user: " + this.userID);
        byte[] signature = CryptoUtils.calculateMAC(pbKey, getSecret(this.userID));
        byte[] encSK = null;
        if (sk != null) {
            Log.i(TAG, "KeyRecord for user: " + this.userID + " have a valid SK");
            encSK = CryptoUtils.encryptSymmetricKey(sk, asymKeyPair.getPublic());
            usersManager.addUser(userID,pbKey, encSK, signature);
            usersManager.authUser(userID);
        } else {
            Log.i(TAG, "KeyRecord for user: " + this.userID + " doesn't have a valid SK yet");
            usersManager.addUser(userID,pbKey, encSK, signature);
        }

    }

    public byte[] getSK() {
        Log.i(TAG, "Searching the KeyRecord for user: " + this.userID);

        // No such user
        if (!usersManager.userExists(this.userID)) {
            Log.w(TAG, "No KeyRecord for user: " + this.userID);
            return null;
        }


        byte[] encSK = usersManager.getSK(this.userID) ;
        // User is not validated yet
        if (encSK == null) {
            Log.i(TAG, "Found an empty encSK for user: " + this.userID);
            return null;
        }

        Log.i(TAG, "Found a valid encSK for user: " + this.userID + ", decrypt it");
        return CryptoUtils.decryptSymmetricKey(encSK, asymKeyPair.getPrivate());
    }

    // Generate list of users
    public HashMap<String, Boolean> getUsers() {
        Log.i(TAG, "Generate list of users");
        HashMap<String, Boolean> users = new HashMap<String, Boolean>();

        for (String curUserID : usersManager.getUsers()) {
            users.put(curUserID,usersManager.userExists(curUserID));
        }

        return users;
    }

    public void authUser(String user) {
        Log.i(TAG, "A User " + user);
        usersManager.authUser(user);
        updated = true;
    }

    // Mark user for removal
    public void removeUser(String user) {
        Log.i(TAG, "Mark User to remove " + user);
        usersManager.markToRemoveUser(user);
        updated = true;

    }

    private boolean validateAllUsers() {
        Log.i(TAG, "Validating all users in the KeyBank");

        boolean userValidated = false;

        // My user doesn't have a valid SK so it can't validate others
        byte[] realSK = getSK();
        if (realSK == null) {
            Log.i(TAG, "My user: " + this.userID + " has no encSK, can't validate others");
            return false;
        }

        // Generate new random SK since user has been removed
        if (usersManager.needsRemoval()) {
            Log.i(TAG, "All Users revalidated, a user has been removed");
            realSK = CryptoUtils.generateRandomSymmetricKey();
            Log.i(TAG, "Real SK Generated: " + realSK.toString());
        }

        // Iterate the users validate them
        for (String curUserID : usersManager.getUsers()) {



            if (usersManager.needsRemoval()) {

                if (usersManager.userShouldBeRemoved(curUserID)) {
//                usersManager.removeUser(curUserID);
                    continue;
                } else {

                    validateUser(realSK, curUserID);
                    userValidated = true;
                }
//                if (!usersManager.userExists(userID))
//                    usersManager.updateSK(realSK);
            } else {

                // No need to validate my user
                if (this.userID.equals(curUserID)) {
//                    if (!usersManager.userExists(userID))
//                        usersManager.addUser(userID);
                    continue;
                }

                // User has an SK, so we don't need to validate it
                if (usersManager.getSK(curUserID) != null) {
                    Log.i(TAG, "User: " + curUserID + " has encSK - no need to validated him");
                    continue;
                } else {
                    Log.i(TAG, "User: " + curUserID + " has no encSK and need to be validated");
                    validateUser(realSK, curUserID);
                    userValidated = true;
                }

            }
        }

        return userValidated;
    }


    private boolean validateUser(byte[] realSK, String userID) {

        // Validate the user's KeyRecord
        boolean valid = validateSignature(userID);

        // If valid, add an encrypted version of SK to the user KeyRecord
        if (valid) {
            Log.i(TAG, "User: " + userID + " has a valid signature, create encSK with his PublicKey");
            PublicKey userPbKey = null;
            final byte[] pbKeyBytes = usersManager.getPbKey(userID);

            try {
                userPbKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM).
                        generatePublic(new X509EncodedKeySpec(pbKeyBytes));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                Log.e(TAG, e.getMessage());
            }

            usersManager.updateSK(userID,CryptoUtils.encryptSymmetricKey(realSK, userPbKey));
            return true;
        } else {
            Log.w(TAG, "User: " + userID + " doesn't have a valid signature, ignore him");
        }
        return false;
    }

    private boolean validateSignature(String userID) {
        return usersManager.userExists(userID) && validateSignature(usersManager.getSignature(userID),
                                                        usersManager.getPbKey(userID),
                                                        userID);
    }

    private boolean validateSignature(byte[] signature, byte[] pbkey, String userID) {

        // Get the user's secret
        byte[] secret = getSecret(userID);

        // Apply the signature process on the given public-key
        byte[] mac = CryptoUtils.calculateMAC(pbkey, secret);

        // Compare the given signature with the real one
        return Arrays.equals(mac, signature);
    }

    private byte[] getSecret(String userID) {
        Log.i(TAG,"Getting Secret for " + userID);
        String secret = usersManager.getSecret(userID);
        if (secret != null)
            return secret.getBytes();
        else
            return null;
    }

    private void readUsersFromKeyBank(String data) {
        Log.i(TAG, "Converting a string to KeyBank");

        for (String curUserID : usersManager.getUsers()) {
            usersManager.markToRemoveUser(curUserID);
        }

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONArray keybank = rootObj.optJSONArray(KEYBANK_TAG);

            // Iterate the JSONArray and init users
            for (int i = 0; i < keybank.length(); i++) {
                JSONObject userObj = keybank.getJSONObject(i);

                // Read user data
                String userID = userObj.optString(USER_ID_ATTR);
                byte[] pbkey = Base64.decode(userObj.optString(PUBLIC_KEY_ATTR), Base64.DEFAULT);
                String encskStr = userObj.optString(ENC_SK_ATTR);
                Log.d(TAG, "JSON encsk: " + encskStr);
                byte[] encsk = encskStr.isEmpty() ? null : Base64.decode(encskStr, Base64.DEFAULT);
                byte[] signature = Base64.decode(userObj.optString(SIGNATURE_ATTR), Base64.DEFAULT);

                if (usersManager.userExists(userID)) {
                    usersManager.markToKeepUser(userID);
                } else {
                    usersManager.addUser(userID, pbkey, encsk, signature);
                    updated = true;
                }

            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());

        }

    }

    public void setUpdated(boolean state) {
        usersManager.usersRemoved();
        updated = state;
    }

    private String keyBankToString()  {
        Log.i(TAG, "Converting a KeyBank to string");
        JSONObject rootObj = new JSONObject();


        try {

            JSONArray keybank = new JSONArray();

            // Iterate the users set and create JSONArray
            for (String userID : usersManager.getValidUsers()) {

                JSONObject userObj = new JSONObject();
                //KeyRecord keyRecord = keyBank.get(userID);
                Log.i(TAG,"Adding user " + userID + " To Serialized KeyBank");
                userObj.put(USER_ID_ATTR, userID);
                userObj.put(PUBLIC_KEY_ATTR, Base64.encodeToString(usersManager.getPbKey(userID), Base64.DEFAULT));
                if (usersManager.getSK(userID) == null) {
                    userObj.put(ENC_SK_ATTR, "");
                } else {
                    userObj.put(ENC_SK_ATTR, Base64.encodeToString(usersManager.getSK(userID), Base64.DEFAULT));
                }
                userObj.put(SIGNATURE_ATTR, Base64.encodeToString(usersManager.getSignature(userID), Base64.DEFAULT));

                keybank.put(userObj);
            }

            // Add the JSONArray to the root JSONObject
            rootObj.put(KEYBANK_TAG, keybank);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return rootObj.toString();
    }

    public String generateEncSKList() {
        Log.i(TAG, "Generating a list of encrypted SKs with all valid public keys");
        JSONObject rootObj = new JSONObject();

        try {

            JSONArray skList = new JSONArray();

            // Iterate the users set and create JSONArray
            for (String userID : usersManager.getValidUsers()) {

                JSONObject skObj = new JSONObject();
                //KeyRecord keyRecord = keyBank.get(userID);
                byte[] pbKeyPrefix = Arrays.copyOf(usersManager.getPbKey(userID), PUBLIC_KEY_PREFIX_SIZE);
                skObj.put(PUBLIC_KEY_PREFIX_ATTR, Base64.encodeToString(pbKeyPrefix, Base64.DEFAULT));
                skObj.put(ENC_SK_ATTR, Base64.encodeToString(usersManager.getSK(userID), Base64.DEFAULT));

                skList.put(skObj);
            }

            // Add the JSONArray to the root JSONObject
            rootObj.put(SKLIST_TAG, skList);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return rootObj.toString();
    }

    public byte[] getSKFromEncSKList(String data) {
        Log.i(TAG, "Searching my SK within the encrypted SKs list");
        if (data == null) {
            Log.i(TAG, "Got an empty data, can't find the SK");
            return null;
        }
        // My PublicKey prefix
        byte[] myPbKeyPrefix = Arrays.copyOfRange(usersManager.getPbKey(this.userID), 0, PUBLIC_KEY_PREFIX_SIZE);

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONArray skList = rootObj.optJSONArray(SKLIST_TAG);

            if (skList == null) {
                Log.w(TAG, "SK list is empty");
                return null;
            }

            // Iterate all SK in the JSONArray
            byte[] curPbKeyPrefix;
            byte[] encSK = null;
            boolean found = false;
            for (int i = 0; i < skList.length(); i++) {
                JSONObject skObj = skList.getJSONObject(i);

                curPbKeyPrefix = Base64.decode(skObj.optString(PUBLIC_KEY_PREFIX_ATTR), Base64.DEFAULT);
                encSK = Base64.decode(skObj.optString(ENC_SK_ATTR), Base64.DEFAULT);

                // Check if this is my public-key
                if (Arrays.equals(myPbKeyPrefix, curPbKeyPrefix)) {
                    Log.i(TAG, "Found my PublicKey within the encrypted SKs list");
                    found = true;
                    break;
                }
            }

            if (found) {
                Log.i(TAG, "Decrypt encSK with my PrivateKey");
                return CryptoUtils.decryptSymmetricKey(encSK, asymKeyPair.getPrivate());
            } else {
                Log.w(TAG, "Couldn't find my PublicKey in the list, return null SK");
                return null;
            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
//
//    protected class KeyBank extends HashMap<String, KeyRecord> {
//
//    }
//
//    protected class KeyRecord {
//
//        public KeyRecord(byte[] pbKey, byte[] encSK, byte[] signature) {
//            this.pbKey = pbKey;
//            this.encSK = encSK;
//            this.signature = signature;
//        }
//
//        protected byte[] pbKey;
//        protected byte[] encSK;
//        protected byte[] signature;
//    }
}