package at.bitfire.davdroid.crypto;

import android.util.Base64;
import android.util.Log;

import net.fortuna.ical4j.model.PropertyList;

import org.json.JSONObject;
import org.spongycastle.crypto.engines.ElGamalEngine;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import ezvcard.util.org.apache.commons.codec.binary.Hex;

public class CryptoUtils {

    /** Tag name */
    private static final String TAG = "CryptoUtils";

    /** The asymmetric crypto algorithm */
    public static final String ASYMMETRIC_ALGORITHM = "ElGamal";

    /** The asymmetric crypto algorithm key size */
    public static final int ASYMMETRIC_KEY_SIZE = 400;

    /** The symmetric crypto algorithm */
    public static final String SYMMETRIC_ALGORITHM = "AES";

    /** The secure random algorithm */
    public static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    /** The symmetric crypto algorithm key size */
    public static final int SYMMETRIC_KEY_SIZE = 128;

    /** The secure hashing algorithm */
    private static final String SIGNATURE_ALGORITHM = "HmacSHA1";

    // Use SpongyCastle (BouncyCastle for Android) as a security provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }


    // TODO for user authentication with asymetric signature use bouncy-castle: elyptic curve, DSA


    /**
     * Generate a random pair (public, private) of keys for the algorithm
     * @return The key pair
     */
    public static KeyPair generateRandomKeyPair() {

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM, "SC");
            SecureRandom random = new SecureRandom();
            generator.initialize(ASYMMETRIC_KEY_SIZE, random);
            return generator.generateKeyPair();

        } catch (Exception e) {
            Log.e(TAG, "generateRandomKeyPair: Fatal error");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt the symmetric key using the asymmetric algorithm
     * @param symmetricKeyBytes The symmetric key to encrypt
     * @param pubKey The asymmetric algorithm public key
     * @return The encrypted symmetric key
     */
    public static byte[] encryptSymmetricKey(byte[] symmetricKeyBytes, PublicKey pubKey) {

        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM + "/None/NoPadding", "SC");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            return cipher.doFinal(symmetricKeyBytes);

        } catch (Exception e) {
            Log.e(TAG, "encryptSymmetricKey: Fatal error");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt the symmetric key using the asymmetric algorithm
     * @param symmetricKeyBytes The encrypted symmetric key to decrypt
     * @param privKey The asymmetric algorithm private key
     * @return The decrypted symmetric key
     */
    public static byte[] decryptSymmetricKey(byte[] symmetricKeyBytes, PrivateKey privKey) {

        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM + "/None/NoPadding", "SC");
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            return cipher.doFinal(symmetricKeyBytes);

        } catch (Exception e) {
            Log.e(TAG, "encryptSymmetricKey: Fatal error");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Generate a random symmetric encryption key
     * @return The key
     */
    public static byte[] generateRandomSymmetricKey() {
        byte[] keyStart = new byte[SYMMETRIC_KEY_SIZE];
        Random rand = new Random();
        rand.nextBytes(keyStart);

        try {

            KeyGenerator generator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            SecureRandom sr = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
            sr.setSeed(keyStart);
            generator.init(SYMMETRIC_KEY_SIZE, sr);
            SecretKey secret = generator.generateKey();
            return secret.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            Log.i(TAG, "NoSuchAlgorithmException");
            return null;
        }
    }

    /**
     *
     * @param raw
     * @param clear
     * @return
     */
    public static byte[] encrypt(byte[] raw, byte[] clear) {
        byte[] encrypted = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            encrypted = cipher.doFinal(clear);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return encrypted;
    }

    public static byte[] decrypt(byte[] raw, byte[] encrypted) {
        byte[] decrypted = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            decrypted = cipher.doFinal(encrypted);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return decrypted;
    }


    /**
     * Calculate the hashed signature of the given data
     * @param data The data
     * @param symmetricKeyBytes The key for the secure hash
     * @return The signature
     */
    public static byte[] calculateSignature(String data, byte[] symmetricKeyBytes) {

        try {
            SecretKeySpec signingKey = new SecretKeySpec(symmetricKeyBytes, SIGNATURE_ALGORITHM);
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(signingKey);
            return mac.doFinal(data.getBytes());
        } catch(NoSuchAlgorithmException ae) {
            Log.i(TAG, "NoSuchAlgorithmException");
            return null;
        } catch (InvalidKeyException ke) {
            Log.i(TAG, "InvalidKeyException");
            return null;
        }
    }

    /**
     * Calculate the MAC of the given data using the given key
     * @param data The data
     * @param symmetricKeyBytes The key for the secure hash
     * @return The MAC
     */
    // TODO use signature without a key, and sign pbke+secret together - send email to Tal
    public static byte[] calculateMAC(byte[] data, byte[] symmetricKeyBytes) {

        try {
            SecretKeySpec signingKey = new SecretKeySpec(symmetricKeyBytes, SIGNATURE_ALGORITHM);
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(signingKey);
            return mac.doFinal(data);
        } catch(NoSuchAlgorithmException ae) {
            Log.i(TAG, "NoSuchAlgorithmException");
            return null;
        } catch (InvalidKeyException ke) {
            Log.i(TAG, "InvalidKeyException");
            return null;
        }
    }

    /**
     * Return the signature size
     * @return The signature size
     */
    public static int signatureSize() {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            return mac.getMacLength();

        } catch(NoSuchAlgorithmException ae) {
            Log.i(TAG, "NoSuchAlgorithmException");
            return -1;
        }
    }

    // TODO: comment
    public static long deriveLong(byte[] key) {
        // TODO: Change to a real crypto algo (spongycastle key derivation?)
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        buffer.put(key, 0, Long.SIZE / Byte.SIZE);
        buffer.flip();//need flip
        return buffer.getLong();
    }
}
