package at.bitfire.davdroid.crypto;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    /** The asymmetric crypto algorithm */
    public static final String ASYMMETRIC_ALGORITHM = "RSA";

    /** The asymmetric crypto algorithm key size */
    public static final int ASYMMETRIC_KEY_SIZE = 1024;

    /** The symmetric crypto algorithm */
    public static final String SYMMETRIC_ALGORITHM = "AES";

    /** The secure random algorithm */
    public static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    /** The symmetric crypto algorithm key size */
    public static final int SYMMETRIC_KEY_SIZE = 128;

    /** The secure hashing algorithm */
    private static final String SIGNATURE_ALGORITHM = "HmacSHA1";

    /**
     * Generate a random pair (public, private) of keys for the algorithm
     * @return The key pair
     */
    public static KeyPair generateRandomKeyPair() {

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
            keyPairGenerator.initialize(ASYMMETRIC_KEY_SIZE);
            return keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
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
            return null;
        }
    }

    /**
     * Encrypt the symmetric key using the asymmetric algorithm
     * @param symmetricKeyBytes The symmetric key to encrypt
     * @param key The asymmetric algorithm public key
     * @return The encrypted symmetric key
     */
    public static byte[] encryptSymmetricKey(byte[] symmetricKeyBytes, PublicKey key) {

        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(symmetricKeyBytes);
        } catch(NoSuchAlgorithmException ae) {
            return null;
        } catch(NoSuchPaddingException pe) {
            return null;
        } catch(InvalidKeyException ke) {
            return null;
        } catch(IllegalBlockSizeException se) {
            return null;
        } catch(BadPaddingException be) {
            return null;
        }
    }

    /**
     * Decrypt the symmetric key using the asymmetric algorithm
     * @param symmetricKeyBytes The encrypted symmetric key to decrypt
     * @param key The asymmetric algorithm private key
     * @return The decrypted symmetric key
     */
    public static byte[] decryptSymmetricKey(byte[] symmetricKeyBytes, PublicKey key) {

        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(symmetricKeyBytes);
        } catch(NoSuchAlgorithmException ae) {
            return null;
        } catch(NoSuchPaddingException pe) {
            return null;
        } catch(InvalidKeyException ke) {
            return null;
        } catch(IllegalBlockSizeException se) {
            return null;
        } catch(BadPaddingException be) {
            return null;
        }
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
            return null;
        } catch (InvalidKeyException ke) {
            return null;
        }
    }
}
