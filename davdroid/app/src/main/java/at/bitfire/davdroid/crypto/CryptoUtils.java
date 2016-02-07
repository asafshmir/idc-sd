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

    /** The symmetric crypto algorithm key size */
    public static final int SYMMETRIC_KEY_SIZE = 128;

    /** The secure hashing algorithm */
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

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
            Log.e(TAG, "generateRandomKeyPair: Fatal error: " + e.getMessage());
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
            Log.e(TAG, "encryptSymmetricKey: Fatal error: " + e.getMessage());
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
            Log.e(TAG, "encryptSymmetricKey: Fatal error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate a random symmetric encryption key
     * @return The key
     */
    public static byte[] generateRandomSymmetricKey() {
        try{
            KeyGenerator keyGen = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            keyGen.init(SYMMETRIC_KEY_SIZE);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generateRandomSymmetricKey: Fatal error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Encrypt the given data
     * @param key The key
     * @param plain The data to encrypt
     * @return The encrypted data
     */
    public static byte[] encrypt(byte[] key, byte[] plain) {

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM + "/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(plain);
        } catch (Exception e) {
            Log.e(TAG, "encrypt: Fatal error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypt the given data
     * @param key The key
     * @param encrypted The data to decrypt
     * @return The decrypted data
     */
    public static byte[] decrypt(byte[] key, byte[] encrypted) {

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM + "/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            Log.e(TAG, "decrypt: Fatal error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculate the MAC of the given data using the given key
     * @param data The data
     * @param symmetricKeyBytes The key for the secure hash
     * @return The MAC
     */
    public static byte[] calculateMAC(byte[] data, byte[] symmetricKeyBytes) {

        try {
            SecretKeySpec signingKey = new SecretKeySpec(symmetricKeyBytes, SIGNATURE_ALGORITHM);
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(signingKey);
            return mac.doFinal(data);

        } catch (Exception e) {
            Log.e(TAG, "calculateMAC: Fatal error: " + e.getMessage());
            return null;
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
