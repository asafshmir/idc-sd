package crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static crypto.CryptoConsts.*;

/**
 * Created by shmir on 1/14/2017.
 */
public class CryptoUtils {

    private static SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        // Initialize a secure random
        SecureRandom random = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
        random.setSeed(System.currentTimeMillis());
        return random;
    }

    /**
     * Create a secret symmetric key
     * @return a SecretKey
     */
    public static SecretKey getSecretKey() throws NoSuchAlgorithmException {
        // Initialize a secure random
        SecureRandom random = getSecureRandom();

        KeyGenerator keyGen = KeyGenerator.getInstance(KEY_GENERATOR_ALGORITHM);
        keyGen.init(random);
        return keyGen.generateKey();
    }

    public static byte[] getRandomBytes(int size) throws NoSuchAlgorithmException {
        SecureRandom random = getSecureRandom();
        byte[] randomBytes = new byte[size];
        random.nextBytes(randomBytes);
        return randomBytes;
    }
}
