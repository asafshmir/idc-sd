import crypto.CryptoVariables;
import crypto.CryptoStorage;
import crypto.CryptoUtils;
import crypto.Utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;
import java.security.cert.CertificateException;

import static crypto.CryptoConsts.*;

/**
 * Created by shmir on 1/14/2017.
 */
public class Encryptor {

    private byte[] iv;
    private Cipher dataCipher;
    private CipherOutputStream dataCipherOs;
    private SecretKey secretKey;

    private CryptoStorage storage;
    private PrivateKey privateKey;

    static private final String VARS_PREFIX = ".vars";
    static private final String ENC_PREFIX = ".enc";

    public Encryptor(String keystorePath, String keystorePassword, String privateKeyAlias)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IOException, InvalidAlgorithmParameterException, UnrecoverableEntryException, KeyStoreException, CertificateException {

        // Initialize CryptoStorage
        storage = new CryptoStorage(keystorePath, keystorePassword);
        privateKey = storage.getPrivateKey(privateKeyAlias);

        // Randomized symmetric keys
        secretKey = CryptoUtils.getSecretKey();
        System.out.println("Using symmetric key: " + Utils.getHexDump(secretKey.getEncoded()));

        // Init dataCipher object with random iv
        dataCipher = Cipher.getInstance(DATA_CIPHER_GENERATOR);
        iv = CryptoUtils.getRandomBytes(dataCipher.getBlockSize());
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        dataCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);


    }

    public void encryptAndSign(String dataFilePath, String certificateAlias) throws IOException, BadPaddingException, IllegalBlockSizeException, SignatureException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {

        // Encrypt and write the encrypted file
        System.out.println("Working on file: " + dataFilePath);
        byte[] plainData = Utils.readFile(dataFilePath);
        FileOutputStream dataOutputStream;
        ObjectOutputStream encOos = null;
        try {
            String encryptedFilePath = dataFilePath + ENC_PREFIX;
            dataOutputStream = new FileOutputStream(encryptedFilePath);
            dataCipherOs = new CipherOutputStream(dataOutputStream, dataCipher);
            encOos = new ObjectOutputStream(dataCipherOs);
            encOos.writeObject(plainData);
            System.out.println("Writing encrypted data to file: " + encryptedFilePath);
            dataCipherOs.write(plainData);
        } finally {
            encOos.close();
        }

        // Get the public key for the given alias
        PublicKey aliasPublicKey = storage.getPublicKeyForCertificate(certificateAlias);

        // Create a CryptoVariables object
        CryptoVariables vars = new CryptoVariables(secretKey, iv, aliasPublicKey, privateKey, plainData);

        // Write the CryptoVariables file
        String varsFilePath = dataFilePath + VARS_PREFIX;
        System.out.println("Writing crypto variables file: " + varsFilePath);
        CryptoVariables.writeVariablesFile(vars, varsFilePath);
        System.out.println("Done!");
    }



    public static void main(String[] args) throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, BadPaddingException, SignatureException, IllegalBlockSizeException {

        // Read arguments from user
        if (args.length != 5) {
            System.out.println("Usage: Encryptor " +
                    "<data_file> <keystore_file> <keystore_password> " +
                    "<private_key_alias> <public_key_alias>");
            return;
        }
        String dataFilePath = args[0];
        String keystorePath = args[1];
        String keystorePassword = args[2];
        String privateKeyAlias = args[3];
        String certificateAlias = args[4];

        // Initialize Encryptor
        Encryptor encryptor = new Encryptor(keystorePath, keystorePassword, privateKeyAlias);

        // Encrypt the given file
        encryptor.encryptAndSign(dataFilePath, certificateAlias);
    }
}