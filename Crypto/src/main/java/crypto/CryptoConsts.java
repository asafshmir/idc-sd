package crypto;

/**
 * Cryptographic consts used by the crypto package.
 */
public class CryptoConsts {

    /** The algorithm for creating secure random */
    public static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    /** The algorithm for symmetric encryption */
    public static final String KEY_GENERATOR_ALGORITHM = "AES";
    public static final String DATA_CIPHER_GENERATOR = "AES/CBC/PKCS5Padding";

    /** The algorithm for encrypting the keys **/
    public static final String KEY_ENCRYPTION_ALGORITHM = "RSA";

    /** The algorithm for signing **/
    public static final String SIGNING_ALGORITHM = "NONEwithRSA";

    /** The type of the keystore **/
    public static final String KEYSTORE_TYPE = "JKS";
}
