package at.bitfire.davdroid.crypto;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
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

    // userID to KeyRecord
    protected Map<String, KeyRecord> keyBank;
    protected KeyPair asymKeyPair;

    public KeyManager(String data)  {

        keyBank = new HashMap<String, KeyRecord>();

        // Sync the asymmetric key-pair
        syncAsymKeyPair();
    }

    private void syncAsymKeyPair() {
        // TODO check if there exists a key-pair in the local storage, and if so read it
        asymKeyPair = CryptoUtils.generateRandomKeyPair();
    }

    public String initKeyBank(String userID, String keyBankData) {

        // TODO remember which user is 'me'

        // KeyBank is empty, create the first record
        if (keyBankData == null) {

            byte[] sk = CryptoUtils.generateRandomSymmetricKey();
            byte[] encSK = CryptoUtils.encryptSymmetricKey(sk, asymKeyPair.getPublic());

            byte[] signedData = new byte[sk.length + encSK.length];
            // TODO figure out which key to use instead of 'secret'
            byte[] signature = CryptoUtils.calculateSignature(signedData, "secret".getBytes());

            keyBank.put(userID, new KeyRecord(asymKeyPair.getPublic().getEncoded(), encSK, signature));

        } else {
            keyBank = stringToKeyBank(keyBankData);
        }

        // TODO if you are the owner, validate all other records and add their encSK

        // Return the new key-bank
        return keyBankToString();
    }

    public byte[] getSK(String userID) {

        KeyRecord keyRecord = keyBank.get(userID);
        if (keyRecord == null) {
            return null;
        }

        byte[] encSK = keyRecord.encSK;
        return CryptoUtils.decryptSymmetricKey(keyRecord.encSK, asymKeyPair.getPrivate());
    }

    public void addUserRequest(String userID, byte[] pbkey) {

    }

    public void validateAllUsers() {

    }

    private Map<String, KeyRecord> stringToKeyBank(String data) {

        Map<String, KeyRecord> keyRecords = new HashMap<String, KeyRecord>();

        try {
            JSONObject rootObj = new JSONObject(data);
            JSONArray keybank = rootObj.optJSONArray(KEYBANK_TAG);

            // Iterate the JSONArray and init users
            for (int i=0; i < keybank.length(); i++){
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
                userObj.put("PUBLIC_KEY_ATTR", Base64.decode(keyRecord.pbkey, Base64.DEFAULT));
                userObj.put("ENC_SK_ATTR", Base64.decode(keyRecord.encSK, Base64.DEFAULT));
                userObj.put("SIGNATURE_ATTR", Base64.decode(keyRecord.signature, Base64.DEFAULT));

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
