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
import java.util.Map;

public class KeyManager {

    private static final String TAG = "davdroid.KeyManager";
    public static final String KEY_STORAGE_EVENT_NAME = "KeyManagerZZ";
    public  static final String EVENT_TIME_FORMAT = "dd-MM-yyyy hh:mm:ss";
    public  static final String KEY_STORAGE_EVENT_TIME = "20-01-2016 00:00:00";
    public  static final String KEY_STORAGE_EVENT_TIME_END = "20-01-2016 23:00:00";

    // Singleton instance
    private static KeyManager instance = null;

    // A map from userID to KeyRecord
    protected String userID;
    // TODO support keyBank per account-name
    // TODO add setActiveAccount to support multiple accounts
    protected Map<String, KeyRecord> keyBank;

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
        keyBank = new HashMap<>();
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
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        }
        return rootObj.toString();
    }

    // TODO handle multiple accounts per KeyManager
    public String initKeyBank(String userID, String keyBankData) {

        this.userID = userID;
        byte[] pbkey = asymKeyPair.getPublic().getEncoded();
        byte[] signature = CryptoUtils.calculateSignature(pbkey, getSecret(this.userID));

        // KeyBank is empty, create the first record
        if (keyBankData == null) {
            Log.i(TAG, "Got an empty KeyBank data, generating random symmetric key");
            byte[] sk = CryptoUtils.generateRandomSymmetricKey();
            byte[] encSK = CryptoUtils.encryptSymmetricKey(sk, asymKeyPair.getPublic());
            keyBank.put(userID, new KeyRecord(pbkey, encSK, signature));

        } else {
            Log.i(TAG, "Got a KeyBank data");
            keyBank = stringToKeyBank(keyBankData);
            // TODO handle corrupted keyBankData?

            // Add a KeyRecord if userID is not in yet in the KeyBank
            // The encSK is empty because this user is not validated yet
            // TODO if you are not the owner, add a request record
            if (!keyBank.containsKey(userID)) {
                keyBank.put(userID, new KeyRecord(pbkey, null, signature));
            }
        }

        // TODO only if you are the owner, validate all other records and add their encSK
        validateAllUsers();

        // Return the new key-bank
        return keyBankToString();
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

    private void validateAllUsers() {
        Log.i(TAG, "Validating all users in the KeyBank");
        // TODO make sure i can validate users - i.e owner
        byte[] realSK = getSK();
        // My user doesn't have a valid SK so it can't validate others
        if (realSK == null)
            return;

        // Iterate the users validate them
        for (String userID : keyBank.keySet()) {

            // No need to validate my user
            if (this.userID.equals(userID))
                continue;

            KeyRecord keyRecord = keyBank.get(userID);
            // User has an SK, so we don't need to validate it
            if (keyRecord.encSK != null)
                continue;

            Log.i(TAG, "User: " + userID + " has no encSK and need to be validated");
            // Validate the user's KeyRecord
            boolean valid = validateSignature(userID, keyRecord);
            final byte[] pbKeyBytes = keyRecord.pbKey;
            // If valid, add an encrypted version of SK to the user KeyRecord
            if (valid) {
                Log.i(TAG, "User: " + userID + " has a valid signature, create encSK with his PublicKey");
                PublicKey userPbKey = null;

                try {
                    userPbKey = KeyFactory.getInstance(CryptoUtils.ASYMMETRIC_ALGORITHM).
                            generatePublic(new X509EncodedKeySpec(pbKeyBytes));
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }

                keyRecord.encSK = CryptoUtils.encryptSymmetricKey(realSK, userPbKey);
            }
        }
    }

    private boolean validateSignature(String userID, KeyRecord keyRecord) {
        return (keyRecord != null) && validateSignature(keyRecord.signature, keyRecord.pbKey, userID);
    }

    private boolean validateSignature(byte[] signature, byte[] pbkey, String userID) {

        // TODO use signature without a key, and sign pbke+secret together

        // Get the user's secret
        byte[] secret = getSecret(userID);

        // Apply the signature process on the given public-key
        byte[] realSignature = CryptoUtils.calculateSignature(pbkey, secret);

        // Compare the given signature with the real one
        return Arrays.equals(realSignature, signature);
    }

    // TODO figure out which key to use instead of 'secret'
    private byte[] getSecret(String userID) {
        return "secret".getBytes();
    }

    private Map<String, KeyRecord> stringToKeyBank(String data) {
        Log.i(TAG, "Converting a string to KeyBank");
        Map<String, KeyRecord> keyRecords = new HashMap<>();

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONArray keybank = rootObj.optJSONArray(KEYBANK_TAG);

            // Iterate the JSONArray and init users
            for (int i = 0; i < keybank.length(); i++) {
                JSONObject userObj = keybank.getJSONObject(i);

                // Read user data
                String userID = userObj.optString(USER_ID_ATTR);
                byte[] pbkey = Base64.decode(userObj.optString(PUBLIC_KEY_ATTR), Base64.DEFAULT);
                byte[] encsk = Base64.decode(userObj.optString(ENC_SK_ATTR), Base64.DEFAULT);
                byte[] signature = Base64.decode(userObj.optString(SIGNATURE_ATTR), Base64.DEFAULT);

                keyRecords.put(userID, new KeyRecord(pbkey, encsk, signature));
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
                userObj.put(ENC_SK_ATTR, Base64.encodeToString(keyRecord.encSK, Base64.DEFAULT));
                userObj.put(SIGNATURE_ATTR, Base64.encodeToString(keyRecord.signature, Base64.DEFAULT));

                keybank.put(userObj);
            }

            // Add the JSONArray to the root JSONObject
            rootObj.put(KEYBANK_TAG, keybank);

        } catch (JSONException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        }
    }

    public class KeyRecord {

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