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
}
