package crypt;

/**
 * Created by shmir on 1/14/2017.
 */
public class CryptoConsts {

    /** The algorithm for creating secure random */
    public static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    /** The algorithm for symmetric encryption */
    public static final String KEY_GENERATOR_ALGORITHM = "AES";
    public static final String DATA_CIPHER_GENERATOR = "AES/CBC/PKCS5Padding";

    /** The type of the keystore **/
    public static final String KEYSTORE_TYPE = "JKS";

    /** The algorithm for signing the data and encrypting the keys **/
    public static final String ASYM_ALGORITHM = "RSA";


}
