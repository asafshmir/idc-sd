package at.bitfire.davdroid.crypto;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyManager {

    public class KeyRecord {

        public KeyRecord(byte[] pbkey, byte[] encSK, byte[] signature) {
            this.pbkey = pbkey;
            this.encSK = encSK;
            this.signature = signature;
        }

        protected byte[] pbkey;
        protected byte[] encSK;
        protected byte[] signature;
    }

    // JSON tag names and attributes
    private final static String KEYBANK_TAG = "KEYBANK";
    private final static String USER_ID_ATTR = "user-id";
    private final static String PUBLIC_KEY_ATTR = "public-key";
    private final static String ENC_SK_ATTR = "enc-sk";
    private final static String SIGNATURE_ATTR = "signature";

    // A map from userID to KeyRecord
    protected String userID;
    protected Map<String, KeyRecord> keyBank;

    // Asymmetric key-pair
    protected KeyPair asymKeyPair;

    // TODO - make key manager singleton
    public KeyManager()  {

        keyBank = new HashMap<String, KeyRecord>();

        // Sync the asymmetric key-pair
        syncAsymKeyPair();
    }

    private void syncAsymKeyPair() {
        // TODO check if there exists a key-pair in the local storage, and if so read it
        asymKeyPair = CryptoUtils.generateRandomKeyPair();
    }

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
    public void addUserRequest(String userID, byte[] pbkey) {

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
            final byte[] pbKeyBytes = keyRecord.pbkey;
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
        return validateSignature(keyRecord.signature, keyRecord.pbkey, userID);
    }

    private boolean validateSignature(byte[] signature, byte[] pbkey, String userID) {

        // TODO use signature without a key, and sign pbkey+secret together

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
                userObj.put("USER_ID_ATTR", userID);
                userObj.put("PUBLIC_KEY_ATTR", Base64.encodeToString(keyRecord.pbkey, Base64.DEFAULT));
                userObj.put("ENC_SK_ATTR", Base64.encodeToString(keyRecord.encSK, Base64.DEFAULT));
                userObj.put("SIGNATURE_ATTR", Base64.encodeToString(keyRecord.signature, Base64.DEFAULT));

                keybank.put(userObj);
            }

            // Add thr JSONArray to the root JSONObject
            rootObj.put(KEYBANK_TAG, keybank);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return rootObj.toString();
    }
}
