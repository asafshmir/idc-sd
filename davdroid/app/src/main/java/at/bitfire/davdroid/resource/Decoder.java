package at.bitfire.davdroid.resource;

import android.util.Base64;
import android.util.Log;

import net.fortuna.ical4j.model.PropertyList;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;

import at.bitfire.davdroid.crypto.CryptoUtils;
import at.bitfire.davdroid.crypto.KeyManager;

public class Decoder {

    /** Tag name */
    private static final String TAG = "Decoder";

    public static void encryptProperty(PropertyList props, byte[] key, String value, Class c) {

        if (value != null && !value.isEmpty()) {
            try {
                Log.d(TAG, "encryptProperty Value: " + value);
                Constructor constructor = c.getConstructor(String.class);
                String encrypted = Base64.encodeToString(CryptoUtils.encrypt(key, value.getBytes()), Base64.DEFAULT);
                props.add(constructor.newInstance(encrypted));

            } catch (Exception e) {
                Log.i(TAG, "Value Failed!");
                e.printStackTrace();
                try {
                    Constructor constructor = c.getConstructor(String.class);
                    // Falling back to not encrypting
                    props.add(constructor.newInstance(value));
                } catch (Exception ex) {
                }
            }
        } else {
            Log.i(TAG, "Value is null!");
        }
    }

    public static String decryptProperty(byte[] key, String value) {
        if(value == null) {
            return null;
        }

        try {
            Log.d(TAG, "decryptProperty Value: " + value);
            String decrypted = new String(CryptoUtils.decrypt(key, Base64.decode(value.getBytes(), Base64.DEFAULT)));
            return  decrypted;
        } catch (Exception e) {
            Log.i(TAG, "Value Failed!");
            e.printStackTrace();
            return null;
        }
    }

    public static void encryptAndSignProperty(PropertyList props, byte[] key, String value, Class c) {

        if (value != null && !value.isEmpty()) {
            try {
                Log.i(TAG, "encryptAndSignProperty Value: " + value);
                Constructor constructor = c.getConstructor(String.class);
                String signature = Base64.encodeToString(CryptoUtils.calculateSignature(value,key), Base64.DEFAULT);
                String encrypted = Base64.encodeToString(CryptoUtils.encrypt(key, value.getBytes()), Base64.DEFAULT);

                // Combine the signature and encrypted data in a JSON object
                JSONObject json = new JSONObject();
                json.put("signature", signature);
                json.put("data", encrypted);
                props.add(constructor.newInstance(json.toString()));

            } catch (Exception e) {
                Log.i(TAG, "Value Failed!");
                e.printStackTrace();
                try {
                    Constructor constructor = c.getConstructor(String.class);
                    // Falling back to not encrypting
                    props.add(constructor.newInstance(value));
                } catch (Exception ex) {
                }
            }
        } else {
            Log.i(TAG, "Value is null!");
        }
    }

    public static boolean checkSignedProperty(byte[] key, String value) {

        if(value == null) {
            return false;
        }

        Log.i(TAG, "checkSignedProperty Value: " + value);

        try {

            // Extract the signature and encrypted data from the JSON object
            JSONObject json = new JSONObject(value);


            String signature = new String(Base64.decode(((String)json.get("signature")).getBytes(), Base64.DEFAULT));
            String decrypted = new String(CryptoUtils.decrypt(key, Base64.decode(((String)json.get("data")).getBytes(), Base64.DEFAULT)));

            String calculated = new String(CryptoUtils.calculateSignature(decrypted, key));

            Log.d(TAG, "checkSignedProperty Equals: " + calculated.equals(signature));

            return calculated.equals(signature);

        } catch (Exception e) {
            Log.i(TAG, "Value Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public static String decryptSignedProperty(byte[] key, String value) {

        if(value == null) {
            return null;
        }

        Log.d(TAG, "decryptSignedProperty Value: " + value);

        try {
            // Extract the encrypted data from the JSON object
            JSONObject json = new JSONObject(value);

            String decrypted = new String(CryptoUtils.decrypt(key, Base64.decode(((String) json.get("data")).getBytes(), Base64.DEFAULT)));
            return decrypted;

        } catch (Exception e) {
            Log.i(TAG, "Value Failed!");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Attach the SK list to the data of a given event property
     * @param key
     * @param data
     * @return
     */
    protected static String attachSKList(byte[] key, String data) {

        if(data == null) {
            return null;
        }

        try {

            // Combine the SK list and encrypted data in a JSON object
            JSONObject json = new JSONObject();
            json.put("sk-list", KeyManager.getInstance().generateEncSKList());
            json.put("data", Base64.encodeToString(CryptoUtils.encrypt(key, data.getBytes()), Base64.DEFAULT));
            Log.i(TAG, "Data: " + json.toString());
            return json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "JSON Failed! - attachSKList");
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Detach the SK list from the given property data and select the current SK
     * @param data
     * @return
     */
    protected static byte[] readAttachedSk(String data) {

        if(data == null) {
            return null;
        }

        try {

            // Get the SK list from the JSON object
            Log.i(TAG, "Data: " + data);
            JSONObject json = new JSONObject(data);
            String skList = (String)json.get("sk-list");
            return KeyManager.getInstance().getSKFromEncSKList(skList);

        } catch (JSONException e) {
            Log.e(TAG, "JSON Failed! - readAttachedSk");
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Detach the original data from the property (discarding SK list information)
     * @param key
     * @param data
     * @return
     */
    protected static String readAttachedData(byte[] key, String data) {

        if(data == null) {
            return null;
        }

        try {

            // Get the SK list from the JSON object
            Log.i(TAG, "Data: " + data);
            JSONObject json = new JSONObject(data);
            String decrypted = new String(decryptProperty(key, ((String) json.get("data"))));
            return decrypted;

        } catch (JSONException e) {
            Log.e(TAG, "JSON Failed! - readAttachedData");
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
