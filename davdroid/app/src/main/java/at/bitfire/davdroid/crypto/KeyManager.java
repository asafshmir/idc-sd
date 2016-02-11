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

// Davka
public class KeyManager {

    private static final String TAG = "davdroid.KeyManager";
    // Constants indicating the location where KeyManager should be saved in the calendar
    public  static final String KEY_STORAGE_EVENT_NAME = "KeyMan";
    public  static final String EVENT_TIME_FORMAT = "dd-MM-yyyy hh:mm:ss";
    public  static final String KEY_STORAGE_EVENT_TIME = "10-02-2016 12:00:00";
    public  static final String KEY_STORAGE_EVENT_TIME_END = "10-02-2016 13:00:00";

    public static boolean isKeyManagerEvent(Event e) {
        return KEY_STORAGE_EVENT_NAME.equals(e.summary);
    }

    // Singleton instance
    private static KeyManager instance = null;

    // A map from userID to KeyRecord
    protected String userID;

    // A class managing the addition and removal of users
    protected UsersManager usersManager;

    // Indicated whether KeyManager has been updated
    @Getter private boolean updated;

    // Asymmetric key-pair
    protected KeyPair asymKeyPair;

    // JSON tag names and attributes
    private final static String KEYBANK_TAG = "key-bank";

    // Constants for the serialization of encryption details into
    // The KeyManager's event and the SK lists in events in general
    private final static String USER_ID_ATTR = "user-id";
    private final static String PUBLIC_KEY_ATTR = "public-key";
    private final static String ENC_SK_ATTR = "enc-sk";
    private final static String SIGNATURE_ATTR = "signature";
    private final static String SKLIST_TAG = "sk-list";
    private final static String PUBLIC_KEY_PREFIX_ATTR = "public-key-prefix";
    private final static String KEYPAIR_TAG = "key-pair";
    private final static String PRIVATE_KEY_ATTR = "private-key";

    private final static int PUBLIC_KEY_PREFIX_SIZE = 64;


    // Default constructor
    private KeyManager() {
        usersManager = new UsersManager();
        updated = false;
    }

    // Singleton
    public static KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }

    /**
     * Read keyPair from given data or generate a new one
     * @param keyPairData the data to read the KeyPair from
     * @return an Asymmetric key pair
     */
    public String syncAsymKeyPair(String keyPairData) {
        if (keyPairData == null) {
            Log.d(TAG, "Got an empty data, generating KeyPair");
            asymKeyPair = CryptoUtils.generateRandomKeyPair();
        } else {
            Log.d(TAG, "Got a KeyPair");
            asymKeyPair = stringToKeyPair(keyPairData);
        }
        return keyPairToString(asymKeyPair);
    }

    /**
     * Read the key pair from a given string
     * @param data the data to read the KeyPair from
     * @return KeyPair read from data
     */
    private KeyPair stringToKeyPair(String data) {
        Log.d(TAG, "Converting a string to KeyPair");
        KeyPair keyPair;

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONObject keyPairObj = rootObj.optJSONObject(KEYPAIR_TAG);
            final byte[] pbKeyData = Base64.decode(keyPairObj.optString(PUBLIC_KEY_ATTR), Base64.DEFAULT);
            final byte[] prKeyData = Base64.decode(keyPairObj.optString(PRIVATE_KEY_ATTR), Base64.DEFAULT);

            // Generate public/private key using spongy castle.
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

    /**
     * serialize an existing KeyPair to its representation
     * @param keyPair the KeyPair to convert to String
     * @return the String representation of the key pair
     */
    private String keyPairToString(KeyPair keyPair) {
        Log.d(TAG, "Converting a KeyPair to string");
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

    /**
     * Read the Key Repository from a given string
     * @param userID indicate who is the current calendar user (owner)
     * @param keyBankData the data encapsulated within the relevant key event in the calendar
     * @return A string representation of the keybank, whether it was updated or not.
     */
    public String initKeyBank(String userID, String keyBankData) {
        updated = false;
        this.userID = userID;
        byte[] pbkey = asymKeyPair.getPublic().getEncoded();

        // KeyBank is empty, create the first record
        if (keyBankData == null) {
            Log.i(TAG, "Got an empty KeyBank data, generating random symmetric key");
            byte[] sk = CryptoUtils.generateRandomSymmetricKey();

            Log.i(TAG, "Adding first user: " + this.userID + " to the KeyBank");
            addKeyRecord(usersManager, userID, pbkey, sk);

            updated = true;
        } else {
            // KeyBank wasn't empty, parse the information, reading the users from it.
            Log.i(TAG, "Got a KeyBank data, parse it");
            readUsersFromKeyBank(keyBankData);

            // this user is not in yet in the KeyBank - add a KeyRecord for him
            // or the user's sk is null because this user is not validated yet
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

    /**
     * Add the relevant keys for a user.
     * @param manager The Users' manager
     * @param userID ID of user to add the relevant keys for
     * @param pbKey User's public key
     * @param sk User's Symmetric key
     */
    private void addKeyRecord(UsersManager manager, String userID, byte[] pbKey, byte[] sk) {

        Log.i(TAG, "Adding KeyRecord to user: " + this.userID);

        // Generate signature of public key
        byte[] signature = CryptoUtils.calculateMAC(pbKey, getSecret(this.userID));

        // If given Symmetric key, encrypt the key and add to use.
        if (sk != null) {
            Log.i(TAG, "KeyRecord for user: " + this.userID + " have a valid SK");
            byte[] encSK = CryptoUtils.encryptSymmetricKey(sk, asymKeyPair.getPublic());
            usersManager.addUser(userID,pbKey, encSK, signature);
            usersManager.authUser(userID);
        } else {
            // Otherwise, simply add a user without an encSK
            Log.i(TAG, "KeyRecord for user: " + this.userID + " doesn't have a valid SK yet");
            usersManager.addUser(userID,pbKey, null, signature);
            if (userID.equals(this.userID)) {
                usersManager.authUser(userID);
            }
        }

    }

    /**
     * Attempt to read Symmetric key for the calendar's user.
     * @return
     */
    public byte[] getSK() {
        Log.i(TAG, "Searching the KeyRecord for user: " + this.userID);

        // No such user
        if (!usersManager.userExists(this.userID)) {
            Log.w(TAG, "No KeyRecord for user: " + this.userID);
            return null;
        }


        byte[] encSK = usersManager.getEncSK(this.userID) ;
        // User is not validated yet
        if (encSK == null) {
            Log.i(TAG, "Found an empty encSK for user: " + this.userID);
            return null;
        }

        Log.i(TAG, "Found a valid encSK for user: " + this.userID + ", decrypt it");
        return CryptoUtils.decryptSymmetricKey(encSK, asymKeyPair.getPrivate());
    }

    /**
     * Generate list of valid users for Account Removal screen
     * @return HashMap of users to boolean
     */
    public HashMap<String, Boolean> getUsers() {
        Log.i(TAG, "Generate list of users");
        HashMap<String, Boolean> users = new HashMap<String, Boolean>();

        for (String curUserID : usersManager.getUsers()) {
            users.put(curUserID,usersManager.isAuthorized(curUserID));
        }

        return users;
    }

    /**
     * Mark user as authorized
     * @param user The user to authorize
     */
    public void authUser(String user) {
        Log.i(TAG, "A User to authorize " + user);
        usersManager.authUser(user);
        updated = true;
    }

    /**
     * Mark user for removal
     * @param user user to mark for removal
     */
    public void removeUser(String user) {
        Log.i(TAG, "Mark User to remove " + user);
        usersManager.markToRemoveUser(user);
        updated = true;

    }

    /**
     * Validate all users added to Calendar
     * @return whether any user has been validated
     */
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

            // If a user needs removal, we update the SK for all users.
            if (usersManager.needsRemoval()) {

                if (usersManager.userShouldBeRemoved(curUserID)) {
                    continue;
                } else {
                    validateUser(realSK, curUserID);
                    userValidated = true;
                }
            } else {
                // No need to validate my user
                if (this.userID.equals(curUserID)) {
                    continue;
                }

                // User has an SK, so we don't need to validate it
                if (usersManager.getEncSK(curUserID) != null) {
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


    /**
     * Validate one user
     * @param realSK The Symmetric key to assign the user
     * @param userID The user to assign a new Symmetric key to
     * @return Whether the user has been validated successfully.
     */
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

    /**
     * Validate the signature for the user
     * @param userID User to update signature for
     * @return whether the signature has been updated
     */
    private boolean validateSignature(String userID) {
        return usersManager.userExists(userID) && validateSignature(usersManager.getSignature(userID),
                                                        usersManager.getPbKey(userID),
                                                        userID);
    }

    /**
     * Validate the signature for the user
     * @param userID User to update signature for
     * @return whether the signature has been updated
     */
    private boolean validateSignature(byte[] signature, byte[] pbkey, String userID) {

        // Get the user's secret
        byte[] secret = getSecret(userID);

        // Apply the signature process on the given public-key
        byte[] mac = CryptoUtils.calculateMAC(pbkey, secret);

        // Compare the given signature with the real one
        return Arrays.equals(mac, signature);
    }

    /**
     * Read the Secret for a specific user
     * @param userID The userID whose secret whould be returned
     * @return the user's secret
     */
    private byte[] getSecret(String userID) {
        Log.i(TAG,"Getting Secret for " + userID);
        String secret = usersManager.getSecret(userID);
        if (secret != null)
            return secret.getBytes();
        else
            return null;
    }

    /**
     * Read the users stored in the Key Event
     * @param data A string representating the Key Event.
     */
    private void readUsersFromKeyBank(String data) {
        Log.i(TAG, "Converting a string to KeyBank");

        // Mark all users to keep, and add each one out
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
                if (userID.equals(this.userID)) {
                    usersManager.authUser(userID);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());

        }

    }

    /**
     * Update the usersManager and KeyManager when the local
     * KeyManager has been updated to the remote calendar.
     */
    public void setUpdated() {
        usersManager.usersRemoved();
        updated = false;
    }

    /**
     * Serialize the KeyManager information into a string representation
     * @return The String representation of the Key Bank.
     */
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

                // If user doesn't have a valid SK, save an empty SK, until someone validates the user
                if (usersManager.getEncSK(userID) == null) {
                    userObj.put(ENC_SK_ATTR, "");
                } else {
                    userObj.put(ENC_SK_ATTR, Base64.encodeToString(usersManager.getEncSK(userID), Base64.DEFAULT));
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

    /**
     * Generate a list of Encrypted Symmetric Keys to be saved in the Encrypted Event
     * @return String containing a list of Encrypted SK
     */
    public String generateEncSKList() {
        Log.i(TAG, "Generating a list of encrypted SKs with all valid public keys");
        JSONObject rootObj = new JSONObject();

        try {

            JSONArray skList = new JSONArray();

            // Iterate the users set and create JSONArray
            for (String userID : usersManager.getValidUsers()) {

                JSONObject skObj = new JSONObject();
                byte[] pbKeyPrefix = Arrays.copyOf(usersManager.getPbKey(userID), PUBLIC_KEY_PREFIX_SIZE);
                skObj.put(PUBLIC_KEY_PREFIX_ATTR, Base64.encodeToString(pbKeyPrefix, Base64.DEFAULT));
                skObj.put(ENC_SK_ATTR, Base64.encodeToString(usersManager.getEncSK(userID), Base64.DEFAULT));

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

    /**
     * Read the Symmetric key from the encrypted list of Symmetric Keys saved in an event
     * @param data A String containing a list of encrypted Symmetric Keys in JSON format
     * @return a byte array of the Symmetric Key
     */
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
}