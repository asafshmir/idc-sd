package at.bitfire.davdroid.crypto;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyManager {

    // Singleton instance
    private static KeyManager instance = null;

    // A map from userID to KeyRecord
    protected String userID;
    // TODO support keyBank per account-name
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
            asymKeyPair = CryptoUtils.generateRandomKeyPair();
        } else {
            asymKeyPair = stringToKeyPair(keyPairData);
        }
        return keyPairToString(asymKeyPair);
    }

    private KeyPair stringToKeyPair(String data) {

        KeyPair keyPair;

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONObject keyPairObj = rootObj.optJSONObject(KEYPAIR_TAG);
            final byte[] pbKeyData = Base64.decode(keyPairObj.optString(PUBLIC_KEY_ATTR).toString(), Base64.DEFAULT);
            final byte[] prKeyData = Base64.decode(keyPairObj.optString(PRIVATE_KEY_ATTR).toString(), Base64.DEFAULT);

            PublicKey pbKey = new PublicKey() {
                @Override
                public String getAlgorithm() {
                    return null;
                }

                @Override
                public String getFormat() {
                    return null;
                }

                @Override
                public byte[] getEncoded() {
                    return pbKeyData;
                }
            };

            PrivateKey prKey = new PrivateKey() {
                @Override
                public String getAlgorithm() {
                    return null;
                }

                @Override
                public String getFormat() {
                    return null;
                }

                @Override
                public byte[] getEncoded() {
                    return prKeyData;
                }
            };

            keyPair = new KeyPair(pbKey, prKey);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return keyPair;
    }

    private String keyPairToString(KeyPair keyPair) {
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

            byte[] sk = CryptoUtils.generateRandomSymmetricKey();
            byte[] encSK = CryptoUtils.encryptSymmetricKey(sk, asymKeyPair.getPublic());
            keyBank.put(userID, new KeyRecord(pbkey, encSK, signature));

        } else {
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

    public byte[] getSK(String userID) {

        KeyRecord keyRecord = keyBank.get(userID);
        // No such user
        if (keyRecord == null)
            return null;

        byte[] encSK = keyRecord.encSK;
        // User is not validated yet
        if (encSK == null)
            return null;

        return CryptoUtils.decryptSymmetricKey(encSK, asymKeyPair.getPrivate());
    }

    public String getBase64SK(String userID) {
        return Base64.encodeToString(getSK(userID),Base64.DEFAULT);
    }

    private void validateAllUsers() {

        // TODO make sure i can validate users - i.e owner
        byte[] realSK = getSK(this.userID);
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

            // Validate the user's KeyRecord
            boolean valid = validateSignature(userID, keyRecord);
            final byte[] pbKeyBytes = keyRecord.pbKey;
            // If valid, add an encrypted version of SK to the user KeyRecord
            if (valid) {
                PublicKey userPbKey = new PublicKey() {
                    @Override
                    public String getAlgorithm() {return null;}
                    @Override
                    public String getFormat() {return null;}
                    @Override
                    public byte[] getEncoded() {return pbKeyBytes;}
                };
                keyRecord.encSK = CryptoUtils.encryptSymmetricKey(realSK, userPbKey);
            }
        }
    }

    private boolean validateSignature(String userID, KeyRecord keyRecord) {
        if (keyRecord == null) return false;
        return validateSignature(keyRecord.signature, keyRecord.pbKey, userID);
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

        Map<String, KeyRecord> keyRecords = new HashMap<String, KeyRecord>();

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONArray keybank = rootObj.optJSONArray(KEYBANK_TAG);

            // Iterate the JSONArray and init users
            for (int i = 0; i < keybank.length(); i++) {
                JSONObject userObj = keybank.getJSONObject(i);

                // Read user data
                String userID = userObj.optString(USER_ID_ATTR).toString();
                byte[] pbkey = Base64.decode(userObj.optString(PUBLIC_KEY_ATTR).toString(), Base64.DEFAULT);
                byte[] encsk = Base64.decode(userObj.optString(ENC_SK_ATTR).toString(), Base64.DEFAULT);
                byte[] signature = Base64.decode(userObj.optString(SIGNATURE_ATTR).toString(), Base64.DEFAULT);

                keyRecords.put(userID, new KeyRecord(pbkey, encsk, signature));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return keyRecords;
    }

    private String keyBankToString()  {

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

        // My PublicKey prefix
        KeyRecord myKeyRecord = keyBank.get(this.userID);
        byte[] myPbKeyPrefix = Arrays.copyOfRange(myKeyRecord.pbKey, 0, PUBLIC_KEY_PREFIX_SIZE);

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONArray skList = rootObj.optJSONArray(SKLIST_TAG);

            // Iterate all SK in the JSONArray
            byte[] curPbKeyPrefix = null;
            byte[] encSK = null;
            for (int i = 0; i < skList.length(); i++) {
                JSONObject skObj = skList.getJSONObject(i);

                curPbKeyPrefix = Base64.decode(skObj.optString(PUBLIC_KEY_PREFIX_ATTR).toString(), Base64.DEFAULT);
                encSK = Base64.decode(skObj.optString(ENC_SK_ATTR).toString(), Base64.DEFAULT);

                // Check if this is my public-key
                if (Arrays.equals(myPbKeyPrefix, curPbKeyPrefix))
                    break;
            }

            return CryptoUtils.decryptSymmetricKey(encSK, asymKeyPair.getPrivate());

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

    @Deprecated
    public String generateEncSKListOld() {

        // Calc total data size
        int numUsers = keyBank.keySet().size();
        int dataSize = numUsers * (PUBLIC_KEY_PREFIX_SIZE + CryptoUtils.SYMMETRIC_KEY_SIZE);
        byte[] data = new byte[dataSize];

        // Create a byte array with the following structure -
        // For each user: PUBLIC_KEY_PREFIX_SIZE first bytes of user's PublicKey + user's SK
        int dataPtr = 0;
        for (String userID : keyBank.keySet()) {
            KeyRecord keyRecord = keyBank.get(userID);
            byte[] pbKey = keyRecord.pbKey;
            byte[] encSK = keyRecord.encSK;
            System.arraycopy(pbKey, 0, data, dataPtr, PUBLIC_KEY_PREFIX_SIZE);
            dataPtr += PUBLIC_KEY_PREFIX_SIZE;
            System.arraycopy(encSK, 0, data, dataPtr, CryptoUtils.SYMMETRIC_KEY_SIZE);
            dataPtr += CryptoUtils.SYMMETRIC_KEY_SIZE;
        }

        // Convert the byte array to Base64
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    @Deprecated
    public byte[] getSKFromEncSKListOld(String allSKData) {

        // Decode Base64 string
        byte[] data = Base64.decode(allSKData.getBytes(), Base64.DEFAULT);
        int dataPtr = 0;

        // My PublicKey prefix
        KeyRecord myKeyRecord = keyBank.get(this.userID);
        byte[] myPbKeyPrefix = Arrays.copyOfRange(myKeyRecord.pbKey, 0, PUBLIC_KEY_PREFIX_SIZE);

        // Parse all SK data and look for my PublicKey prefix
        // TODO make sure that data's length is valid
        int numSK = data.length / (PUBLIC_KEY_PREFIX_SIZE + CryptoUtils.SYMMETRIC_KEY_SIZE);
        for (int i = 0; i < numSK; i++) {
            byte[] curPbKeyPrefix = Arrays.copyOfRange(data, dataPtr, dataPtr + PUBLIC_KEY_PREFIX_SIZE);
            dataPtr += PUBLIC_KEY_PREFIX_SIZE;

            // Check if this is my public-key
            if (Arrays.equals(myPbKeyPrefix, curPbKeyPrefix))
                break;

            dataPtr += CryptoUtils.SYMMETRIC_KEY_SIZE;
        }
        // TODO handle a situation where pkKeyPrefix is not found
        // Get the matching version of the encSK that can be decrypted with my private key
        byte[] encSK = Arrays.copyOfRange(data, dataPtr, dataPtr + CryptoUtils.SYMMETRIC_KEY_SIZE);
        return CryptoUtils.decryptSymmetricKey(encSK, asymKeyPair.getPrivate());
    }

    @Deprecated
    private String keyPairToStringOld(KeyPair keyPair) {
        byte[] pbKeyData = keyPair.getPublic().getEncoded();
        byte[] prKeyData = keyPair.getPrivate().getEncoded();
        byte[] data = new byte[pbKeyData.length + prKeyData.length];
        System.arraycopy(pbKeyData, 0, data, 0, pbKeyData.length);
        System.arraycopy(prKeyData, 0, data, pbKeyData.length, prKeyData.length);
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    @Deprecated
    private KeyPair stringToKeyPairOld(String keyPairData) {
        byte[] data = Base64.decode(keyPairData.getBytes(), Base64.DEFAULT);
        final byte[] pbKeyData = new byte[CryptoUtils.ASYMMETRIC_KEY_SIZE];
        final byte[] prKeyData = new byte[CryptoUtils.ASYMMETRIC_KEY_SIZE];
        System.arraycopy(data, 0, pbKeyData, 0, pbKeyData.length);
        System.arraycopy(data, pbKeyData.length, prKeyData, 0, prKeyData.length);

        PublicKey pbKey = new PublicKey() {
            @Override
            public String getAlgorithm() { return null; }
            @Override
            public String getFormat() { return null; }
            @Override
            public byte[] getEncoded() { return pbKeyData; }
        };

        PrivateKey prKey = new PrivateKey() {
            @Override
            public String getAlgorithm() { return null; }
            @Override
            public String getFormat() { return null; }
            @Override
            public byte[] getEncoded() { return prKeyData; }
        };

        return new KeyPair(pbKey, prKey);
    }

}
