package crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static crypto.CryptoConsts.*;

/**
 * Usefull cryptographic utils used by the crypto package.
 */
public class CryptoUtils {

    /**
     * Create a secure random using a defines secure random algorithm
     * @return a {@link SecureRandom} object
     */
    private static SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        // Initialize a secure random
        SecureRandom random = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
        random.setSeed(System.currentTimeMillis());
        return random;
    }

    /**
     * Create a random secret symmetric key and return it
     * @return the random secret symmetric key
     */
    public static SecretKey getSecretKey() throws NoSuchAlgorithmException {
        // Initialize a secure random
        SecureRandom random = getSecureRandom();

        KeyGenerator keyGen = KeyGenerator.getInstance(KEY_GENERATOR_ALGORITHM);
        keyGen.init(random);
        return keyGen.generateKey();
    }

    /**
     * Generates a given number of random bytes
     * @param size number of bytes to create
     * @return random bytes
     */
    public static byte[] getRandomBytes(int size) throws NoSuchAlgorithmException {
        SecureRandom random = getSecureRandom();
        byte[] randomBytes = new byte[size];
        random.nextBytes(randomBytes);
        return randomBytes;
    }
}
