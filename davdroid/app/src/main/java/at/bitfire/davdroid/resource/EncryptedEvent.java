package at.bitfire.davdroid.resource;

import android.util.Log;

import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;

import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by jonatan on 18/11/15.
 */
public class EncryptedEvent extends Event {
    private final static String TAG = "davdroid.EncryptedEvent";


    protected byte[] key;

    public EncryptedEvent(String name, String ETag) {
        super(name, ETag);
        key = generateKey("this is a key");
    }

    public EncryptedEvent(long localID, String name, String ETag) {
        super(localID, name, ETag);
        key = generateKey("this is a key");
    }


    private byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        Log.d(TAG, "skeySpec");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        Log.d(TAG,"initCipher");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        Log.d(TAG,"doFinal");
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    private byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }


    protected byte[] generateKey(String key) {
        try {
            byte[] keyStart = key.getBytes();
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(keyStart);
            kgen.init(128, sr); // 192 and 256 bits may not be available
            SecretKey skey = kgen.generateKey();
            return skey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            return "FallBackKey".getBytes();
        }
    }

    protected boolean encryptProperty(PropertyList props, byte[] key, String value, Class c) {

        if (value != null && !value.isEmpty()) {
            try {
                Constructor constructor = c.getConstructor(String.class);
                props.add(constructor.newInstance(encrypt(key, value.getBytes())));

            } catch (Exception e) {
                try {
                    Constructor constructor = c.getConstructor(String.class);
                    // Falling back to not encrypting
                    props.add(constructor.newInstance(value));
                } catch (Exception ex) {
                }
            }
        }

        // TODO - change this
        return true;
    }
    protected VEvent toVEvent() {
        VEvent event = new VEvent();
        PropertyList props = event.getProperties();

        if (uid != null)
            props.add(new Uid(uid));
        if (recurrenceId != null)
            props.add(recurrenceId);

        props.add(dtStart);
        if (dtEnd != null)
            props.add(dtEnd);
        if (duration != null)
            props.add(duration);

        if (rrule != null)
            props.add(rrule);
        if (rdate != null)
            props.add(rdate);
        if (exrule != null)
            props.add(exrule);
        if (exdate != null)
            props.add(exdate);

        encryptProperty(props, key, summary, Summary.class);
        encryptProperty(props, key, location, Location.class);
        encryptProperty(props, key, description, Description.class);


        if (status != null)
            props.add(status);
        if (!opaque)
            props.add(Transp.TRANSPARENT);

        encryptProperty(props, key, description, Organizer.class);

        props.addAll(attendees);

        if (forPublic != null)
            event.getProperties().add(forPublic ? Clazz.PUBLIC : Clazz.PRIVATE);

        event.getAlarms().addAll(alarms);

        props.add(new LastModified());
        return event;
    }


}
