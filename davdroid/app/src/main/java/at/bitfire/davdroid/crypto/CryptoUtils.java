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
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CryptoUtils {

    /** The asymmetric crypto algorithm */
    public static final String ALGORITHM = "RSA";

    /** The asymmetric crypto algorithm key size */
    public static final int KEY_SIZE = 1024;

    /**
     * Generate a random pair (public, private) of keys for the algorithm
     * @return The key pair
     */
    public static KeyPair generateRandomKeyPair() {

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            return keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Generate a random symmetric encryption key
     * @param size The wanted key size
     * @return The key
     */
    public static byte[] generateRandomSymmetricKey(int size) {
        byte[] key = new byte[size];
        Random rand = new Random();
        rand.nextBytes(key);
        return key;
    }

    /**
     * Encrypt the symmetric key using the asymmetric algorithm
     * @param symmetricKeyBytes The symmetric key to encrypt
     * @param key The asymmetric algorithm public key
     * @return The encrypted symmetric key
     */
    public static byte[] encryptSymmetricKey(byte[] symmetricKeyBytes, PublicKey key) {

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
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
            Cipher cipher = Cipher.getInstance(ALGORITHM);
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

    public static byte[] generateKey(byte[] keyStart) {
        try {

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
}
