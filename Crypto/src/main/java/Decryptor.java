import crypto.CryptoVariables;
import crypto.CryptoStorage;
import crypto.Utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

import static crypto.CryptoConsts.*;

/**
 * Decrypt a given file using a symmetric key encryption,
 * obtain the encrypted symmetric key from a var file and decrypt it with a private key,
 * verifies a digital signature on the decrypted file using a trusted public key.
 */
public class Decryptor {

    /** A cipher object */
    private Cipher dataDecipher;
    /** Allows access to the file holding the certificates */
    private CryptoStorage storage;
    /** Private key stored in the {@link CryptoStorage} */
    private PrivateKey privateKey;

    /** Output messages */
    static private final String SIGN_ERR_MSG = "Signature verification failed!";
    static private final String SIGN_SUCCESS_MSG = "Signature verification succeeded!";

    /**
     * The main constructor for {@link Decryptor}
     * @param keystorePath the keystore file holding all the certificates and private key
     * @param keystorePassword the password protecting the keystore file
     * @param privateKeyAlias an alias for the own certificate within the keystore file
     */
    public Decryptor(String keystorePath, String keystorePassword, String privateKeyAlias)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IOException, InvalidAlgorithmParameterException, UnrecoverableEntryException, KeyStoreException, CertificateException {

        // Initialize CryptoStorage
        storage = new CryptoStorage(keystorePath, keystorePassword);
        privateKey = storage.getPrivateKey(privateKeyAlias);

        // Init dataCipher object
        dataDecipher = Cipher.getInstance(DATA_CIPHER_GENERATOR);
    }

    /**
     * Decrypt a given file using a symmetric key encryption,
     * obtain the encrypted symmetric key from a var file and decrypt it with a private key,
     * verifies a digital signature on the decrypted file using a trusted public key.
     * @param encDataFilePath the file to decrypt
     * @param varsFilePath the file holding the relevant variables for decrypting (encrypted symmetric key, iv)
     * @param certificateAlias an alias for the other side’s certificate (for using the proper public key)
     * @param outputDataFilePath the file to write the decrypted data to
     */
    public void decryptAndValidate(String encDataFilePath, String varsFilePath,
                                   String certificateAlias, String outputDataFilePath)
            throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, KeyStoreException, SignatureException {

        System.out.println("Working on file: " + encDataFilePath);

        // Read decryption parameters
        System.out.println("Reading crypto variables file: " + varsFilePath);
        CryptoVariables vars = CryptoVariables.readVariablesFile(varsFilePath);
        byte[] iv = vars.iv;

        // Init cipher object for decrypting the symmetric key
        SecretKey secretKey = vars.decryptKey(privateKey);
        //System.out.println("Using symmetric key: " + Utils.getHexDump(secretKey.getEncoded()));

        // Decrypt the encrypted file
        FileInputStream fis = new FileInputStream(encDataFilePath);
        dataDecipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        CipherInputStream cis = new CipherInputStream(fis, dataDecipher);
        ObjectInputStream cipherOis = new ObjectInputStream(cis);
        byte[] plainData = (byte[]) cipherOis.readObject();

        // Get the public key for the given alias
        PublicKey aliasPublicKey = storage.getPublicKeyForCertificate(certificateAlias);

        // Verify the signature
        boolean verifies = vars.verifySignature(aliasPublicKey, plainData);
        if (verifies) {
            System.out.println(SIGN_SUCCESS_MSG);
        } else {
            System.err.println(SIGN_ERR_MSG);
            plainData = SIGN_ERR_MSG.getBytes();
        }

        // Write the decrypted data
        FileOutputStream fos = new FileOutputStream(outputDataFilePath);
        fos.write(plainData);
        fos.close();

        System.out.println("Done!");
    }

    /**
     * Decrypt a given file using a symmetric key encryption,
     * obtain the encrypted symmetric key from a var file and decrypt it with a private key,
     * verifies a digital signature on the decrypted file using a trusted public key.
     * Usage: Decryptor <encrypted_file> <crypto_vars_file> <keystore_file> <keystore_password> <private_key_alias>
     *     <public_key_alias> <output_file>
     * encrypted_file – the file to decrypt
     * crypto_vars_file - the file holding the relevant variables for decrypting (encrypted symmetric key, iv)
     * keystore_file – the keystore file holding all the certificates and private key
     * keystore_password – the password protecting the keystore file
     * private_key_alias – an alias for the own certificate within the keystore file
     * public_key_alias – an alias for the other side’s certificate (for using the proper public key)
     * output_file - the file to write the decrypted data to
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, BadPaddingException, SignatureException, IllegalBlockSizeException,
            ClassNotFoundException {

        // Read arguments from user
        if (args.length != 7) {
            System.out.println("Usage: Decryptor " +
                    "<encrypted_file> <crypto_vars_file> <keystore_file> " +
                    "<keystore_password> <private_key_alias> <public_key_alias> <output_file>");
            return;
        }
        String encDataFilePath = args[0];
        String varsFilePath = args[1];
        String keystorePath = args[2];
        String keystorePassword = args[3];
        String privateKeyAlias = args[4];
        String certificateAlias = args[5];
        String outputDataFilePath = args[6];

        // Initialize Decryptor
        Decryptor decryptor = new Decryptor(keystorePath, keystorePassword, privateKeyAlias);

        // Decrypt the given file and validate signature
        decryptor.decryptAndValidate(encDataFilePath, varsFilePath, certificateAlias, outputDataFilePath);
    }

}
