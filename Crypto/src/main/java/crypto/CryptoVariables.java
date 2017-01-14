package crypto;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

import static crypto.CryptoConsts.*;

/**
 * Created by shmir on 1/14/2017.
 */
public class CryptoVariables implements java.io.Serializable {

    private static final long serialVersionUID = 8102302223781025809L;

    public byte[] signature;
    public byte[] encryptedKeyEncoded;
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

        this.encryptedKeyEncoded = encryptKey(secretKey, publicKey);
        this.iv = iv;
        this.signature = signData(privateKey, plainData);
    }

    /**
     * Decrypt the encoded secret key's bytes by RSA using the argument
     * RSAPrivateKey.<br>
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
        byte[] decryptedEncodedKey = keyDecryptor.doFinal(encryptedKeyEncoded);
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

    public boolean verifySignature(PublicKey publicKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(SIGNING_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        boolean verifies = signature.verify(this.signature);
        return verifies;
    }

    public static void writeVariablesFile(CryptoVariables cryptoConf, String configFilePath) throws IOException {
        ObjectOutputStream out = null;
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(configFilePath);
            out = new ObjectOutputStream(fileOut);
            out.writeObject(cryptoConf);
            out.flush();
        } finally {
            out.close();
            fileOut.close();
        }
    }

    public static CryptoVariables readVariablesFile(String configFilePath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        FileInputStream fileIn = null;
        try {
            fileIn = new FileInputStream(configFilePath);
            in = new ObjectInputStream(fileIn);
            return (CryptoVariables) in.readObject();
        } finally {
            in.close();
            fileIn.close();
        }
    }
}