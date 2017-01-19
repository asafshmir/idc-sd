package crypto;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

import static crypto.CryptoConsts.*;

/**
 * Represents the set of cryptographic variables used for encrypting and decrypting.
 * Supply methods for encrypting and decrypting the symmetric key which is encrypted by
 * an asymmetric encryption.
 * {@link CryptoVariables} is also {@link Serializable} which allows to read and write from\to file.
 */
public class CryptoVariables implements java.io.Serializable {

    private static final long serialVersionUID = 8102302223781025809L;

    /** The digital signature of the data file */
    public byte[] signature;
    /** The encrypted symmetric key using a trusted public key */
    public byte[] encryptedKeyBytes;
    /** A random IV used for encryption */
    public byte[] iv;

    /**
     * Construct an instance using parameters
     *
     * @param secretKey The SecretKey to use for the encryption/decryption of the file's data
     * @param iv The iv to use for decrypting to the data
     * @param publicKey The public key to use to encrypt the arguement secretKey
     * @param privateKey The private key used for signing the data
     * @param plainData The plain data
     */
    public CryptoVariables(SecretKey secretKey, byte[] iv, Key publicKey,
                           PrivateKey privateKey, byte[] plainData)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, SignatureException {

        this.encryptedKeyBytes = encryptKey(secretKey, publicKey);
        this.iv = iv;
        this.signature = signData(privateKey, plainData);
    }

    /**
     * Decrypt the encoded secret key's bytes by RSA using the argument
     * RSAPrivateKey.
     *
     * @param privateKey The public key to use when decrypting the secret key
     * @return the decrypted SecretKeySpec which consists of the decrypted bytes
     *         of the key and the algorithm which is defined is
     */
    public SecretKey decryptKey(Key privateKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher keyDecryptor = Cipher.getInstance(KEY_ENCRYPTION_ALGORITHM);
        keyDecryptor.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedEncodedKey = keyDecryptor.doFinal(encryptedKeyBytes);
        return new SecretKeySpec(decryptedEncodedKey, KEY_GENERATOR_ALGORITHM);
    }

    /**
     * Encrypt the secretKey by RSA using the argument public key
     *
     * @param secretKey The SecretKey to encrypt
     * @param publicKey The public key to use for the encryption
     * @return the encrypted encoded bytes
     */
    private byte[] encryptKey(SecretKey secretKey, Key publicKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher keyEncryptor = Cipher.getInstance(KEY_ENCRYPTION_ALGORITHM);
        keyEncryptor.init(Cipher.ENCRYPT_MODE, publicKey);
        return keyEncryptor.doFinal(secretKey.getEncoded());
    }

    /**
     * Sign the plain data before encryption with a given PrivateKey
     * @param privateKey The private key to sign with
     * @param data The plain data
     * @return The signed data
     */
    private byte[] signData(PrivateKey privateKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(SIGNING_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verify the read signature with the given data and public key
     * @param publicKey the public key corresponded with the private key used for signing the data
     * @param data the signed data
     * @return true if the signature is verified, and false otherwise
     */
    public boolean verifySignature(PublicKey publicKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(SIGNING_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        boolean verifies = signature.verify(this.signature);
        return verifies;
    }

    /**
     * Write the {@link CryptoVariables} object to a file
     * @param cryptoVars The {@link CryptoVariables} object to write
     * @param varsFilePath The path to write the object
     */
    public static void writeVariablesFile(CryptoVariables cryptoVars, String varsFilePath) throws IOException {
        ObjectOutputStream out = null;
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(varsFilePath);
            out = new ObjectOutputStream(fileOut);
            out.writeObject(cryptoVars);
            out.flush();
        } finally {
            out.close();
            fileOut.close();
        }
    }

    /**
     * Reads a {@link CryptoVariables} from a given file
     * @param varsFilePath The file where the {@link CryptoVariables} object is stored
     * @return The read {@link CryptoVariables} object
     */
    public static CryptoVariables readVariablesFile(String varsFilePath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        FileInputStream fileIn = null;
        try {
            fileIn = new FileInputStream(varsFilePath);
            in = new ObjectInputStream(fileIn);
            return (CryptoVariables) in.readObject();
        } finally {
            in.close();
            fileIn.close();
        }
    }
}