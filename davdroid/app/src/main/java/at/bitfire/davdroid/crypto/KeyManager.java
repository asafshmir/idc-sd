package at.bitfire.davdroid.crypto;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
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
    public static final String KEY_STORAGE_EVENT_NAME = "KeyManagerTT";
    public  static final String EVENT_TIME_FORMAT = "dd-MM-yyyy hh:mm:ss";
    public  static final String KEY_STORAGE_EVENT_TIME = "04-02-2016 00:00:00";
    public  static final String KEY_STORAGE_EVENT_TIME_END = "04-02-2016 23:00:00";
    //TODO move to SyncManager
    public static boolean isKeyManagerEvent(Event e) {
        return e.summary.equals(KEY_STORAGE_EVENT_NAME);
    }

    // Singleton instance
    private static KeyManager instance = null;

    // A map from userID to KeyRecord
    protected String userID;
    // TODO support keyBank per account-name
    // TODO add setActiveAccount to support multiple accounts
    //protected Map<String, KeyRecord> keyBank;
    protected KeyBank keyBank;
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
        keyBank = new KeyBank();
        usersManager = new DummyUsersManager();
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
            pbKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM).generatePublic(new X509EncodedKeySpec(pbKeyData));
            PrivateKey prKey;
            prKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(prKeyData));

            keyPair = new KeyPair(pbKey, prKey);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return keyPair;
    }

    private String keyPairToString(KeyPair keyPair) {
        Log.i(TAG, "Converting a KeyPair to string");
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

        this.userID = userID;
        byte[] pbkey = asymKeyPair.getPublic().getEncoded();

        // KeyBank is empty, create the first record
        if (keyBankData == null) {
            Log.i(TAG, "Got an empty KeyBank data, generating random symmetric key");
            byte[] sk = CryptoUtils.generateRandomSymmetricKey();
            Log.i(TAG, "Adding first user: " + this.userID + " to the KeyBank");
            addKeyRecord(keyBank, userID, pbkey, sk);
            updated = true;
        } else {
            Log.i(TAG, "Got a KeyBank data, parse it");
            keyBank = stringToKeyBank(keyBankData);
            // TODO handle corrupted keyBankData?

            // userID is not in yet in the KeyBank - add a KeyRecord for him
            // The sk is null because this user is not validated yet
            if (!keyBank.containsKey(userID)) {
                Log.i(TAG, "User: " + this.userID + " doesn't exist in KeyBank, add it");
                addKeyRecord(keyBank,userID, pbkey, null);
            // userID exists
            } else {
                // Make sure that the SK is valid - maybe userID lost his private key
                if (getSK() != null) {
                    Log.w(TAG, "User: " + this.userID + " already exists, and has a valid SK");
                } else {
                    Log.w(TAG, "User: " + this.userID + " has an SK, but can't decrypt it. Add a new KeyRecord for him");
                    addKeyRecord(keyBank,userID, pbkey, null);
                }
            }
        }

        // Try to validate other users
        updated = updated || validateAllUsers();

        // Return the new key-bank
        return keyBankToString();
    }

    private void addKeyRecord(KeyBank kb, String userID, byte[] pbkey, byte[] sk) {
        Log.i(TAG, "Adding KeyRecord to user: " + this.userID);
        byte[] signature = CryptoUtils.calculateMAC(pbkey, getSecret(this.userID));
        byte[] encSK = null;
        if (sk != null) {
            Log.i(TAG, "KeyRecord for user: " + this.userID + " have a valid SK");
            encSK = CryptoUtils.encryptSymmetricKey(sk, asymKeyPair.getPublic());
        } else {
            Log.i(TAG, "KeyRecord for user: " + this.userID + " doesn't have a valid SK yet");
        }
        kb.put(userID, new KeyRecord(pbkey, encSK, signature));
    }

    public byte[] getSK() {
        Log.i(TAG, "Searching the KeyRecord for user: " + this.userID);
        KeyRecord keyRecord = keyBank.get(this.userID);
        // No such user
        if (keyRecord == null) {
            Log.w(TAG, "No KeyRecord for user: " + this.userID);
            return null;
        }

        byte[] encSK = keyRecord.encSK;
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

        // Iterate the users, find if validated
        for (String curUserID : keyBank.keySet()) {
            KeyRecord keyRecord = keyBank.get(curUserID);
            if (keyRecord.encSK != null) {
                users.put(curUserID,true);
            } else {
                users.put(curUserID,false);
            }
        }

        return users;
    }

    // update new SK for users after deletion
    public void updateUsers(HashMap<String, Boolean> users) {

        Log.i(TAG, "Generate list of users");

        boolean shouldUpdateUsers = false;
        for (String curUserID : users.keySet()) {
            if (users.get(curUserID) == false) {
                shouldUpdateUsers = true;
                break;
            }
        }

        if (shouldUpdateUsers) {
            KeyBank newKeyBank = new KeyBank();
            byte[] sk = CryptoUtils.generateRandomSymmetricKey();

            // Iterate the users, find if validated
            for (String curUserID : users.keySet()) {
                if ((users.get(curUserID) == true) ||
                        (curUserID == this.userID)) {
                    KeyRecord record = keyBank.get(curUserID);
                    addKeyRecord(newKeyBank, curUserID, record.pbKey, sk);
                }
            }

            keyBank = newKeyBank;
            updated = true;
        }
    }

    private boolean validateAllUsers() {
        Log.i(TAG, "Validating all users in the KeyBank");


        // My user doesn't have a valid SK so it can't validate others
        byte[] realSK = getSK();
        if (realSK == null) {
            Log.i(TAG, "My user: " + this.userID + " has no encSK, can't validate others");
            return false;
        }

        // Iterate the users validate them
        for (String curUserID : keyBank.keySet()) {

            // No need to validate my user
            if (this.userID.equals(curUserID))
                continue;

            KeyRecord keyRecord = keyBank.get(curUserID);
            // User has an SK, so we don't need to validate it
            if (keyRecord.encSK != null) {
                Log.i(TAG, "User: " + curUserID + " has encSK - no need to validated him");
                continue;
            } else {
                Log.i(TAG, "User: " + curUserID + " has no encSK and need to be validated");
                return validateUser(realSK, curUserID, keyRecord);

            }
        }
        return false;
    }

    private boolean validateUser(byte[] realSK, String userID, KeyRecord keyRecord) {

        // Validate the user's KeyRecord
        boolean valid = validateSignature(userID, keyRecord);

        // If valid, add an encrypted version of SK to the user KeyRecord
        if (valid) {
            Log.i(TAG, "User: " + userID + " has a valid signature, create encSK with his PublicKey");
            PublicKey userPbKey = null;
            final byte[] pbKeyBytes = keyRecord.pbKey;

            try {
                userPbKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM).
                        generatePublic(new X509EncodedKeySpec(pbKeyBytes));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                Log.e(TAG, e.getMessage());
            }

            keyRecord.encSK = CryptoUtils.encryptSymmetricKey(realSK, userPbKey);
            return true;
        } else {
            Log.w(TAG, "User: " + userID + " doesn't have a valid signature, ignore him");
        }
        return false;
    }

    private boolean validateSignature(String userID, KeyRecord keyRecord) {
        return (keyRecord != null) && validateSignature(keyRecord.signature, keyRecord.pbKey, userID);
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
        String secret = usersManager.getSecret(userID);
        return secret.getBytes();
    }

    private KeyBank stringToKeyBank(String data) {
        Log.i(TAG, "Converting a string to KeyBank");
        KeyBank keyRecords = new KeyBank();

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

                keyRecords.put(userID, new KeyRecord(pbkey, encsk, signature));
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return keyRecords;
    }

    private String keyBankToString()  {
        Log.i(TAG, "Converting a KeyBank to string");
        JSONObject rootObj = new JSONObject();

        try {

            JSONArray keybank = new JSONArray();

            // Iterate the users set and create JSONArray
            for (String userID : keyBank.keySet()) {

                JSONObject userObj = new JSONObject();
                KeyRecord keyRecord = keyBank.get(userID);
                userObj.put(USER_ID_ATTR, userID);
                userObj.put(PUBLIC_KEY_ATTR, Base64.encodeToString(keyRecord.pbKey, Base64.DEFAULT));
                if (keyRecord.encSK == null) {
                    userObj.put(ENC_SK_ATTR, "");
                } else {
                    userObj.put(ENC_SK_ATTR, Base64.encodeToString(keyRecord.encSK, Base64.DEFAULT));
                }
                userObj.put(SIGNATURE_ATTR, Base64.encodeToString(keyRecord.signature, Base64.DEFAULT));

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
            for (String userID : keyBank.keySet()) {

                JSONObject skObj = new JSONObject();
                KeyRecord keyRecord = keyBank.get(userID);
                byte[] pbKeyPrefix = Arrays.copyOf(keyRecord.pbKey, PUBLIC_KEY_PREFIX_SIZE);
                skObj.put(PUBLIC_KEY_PREFIX_ATTR, Base64.encodeToString(pbKeyPrefix, Base64.DEFAULT));
                skObj.put(ENC_SK_ATTR, Base64.encodeToString(keyRecord.encSK, Base64.DEFAULT));

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
        KeyRecord myKeyRecord = keyBank.get(this.userID);
        byte[] myPbKeyPrefix = Arrays.copyOfRange(myKeyRecord.pbKey, 0, PUBLIC_KEY_PREFIX_SIZE);

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

    protected class KeyBank extends HashMap<String, KeyRecord> {

    }

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
}