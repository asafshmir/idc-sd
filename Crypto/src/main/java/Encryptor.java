import crypt.CryptoConfiguration;
import crypt.CryptoStorage;
import crypt.CryptoUtils;
import crypt.Utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;
import java.security.cert.CertificateException;

import static crypt.CryptoConsts.*;

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
    private Signature asymSign;

    static private final String CONF_PREFIX = ".conf";
    static private final String ENC_PREFIX = ".enc";

    public Encryptor(String keystorePath, String keystorePassword, String privateKeyAlias)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IOException, InvalidAlgorithmParameterException, UnrecoverableEntryException, KeyStoreException, CertificateException {

        // Initialize CryptoStorage
        storage = new CryptoStorage(keystorePath, keystorePassword);
        privateKey = storage.getPrivateKey(privateKeyAlias);

        // Randomized symmetric keys
        secretKey = CryptoUtils.getSecretKey();

        // Init dataCipher object with random iv
        dataCipher = Cipher.getInstance(DATA_CIPHER_GENERATOR);
        iv = CryptoUtils.getRandomBytes(dataCipher.getBlockSize());
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        dataCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

        // Init a signature object
        asymSign = Signature.getInstance(ASYM_ALGORITHM);
        asymSign.initSign(privateKey);
    }

    private void encryptAndSign(String dataFilePath, String certificateAlias) throws IOException, BadPaddingException, IllegalBlockSizeException, SignatureException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {

        // Encrypt and write the encrypted file
        byte[] plainData = Utils.readFile(dataFilePath);
        FileOutputStream dataOutputStream;
        ObjectOutputStream encOos = null;
        try {
            dataOutputStream = new FileOutputStream(dataFilePath + ENC_PREFIX);
            dataCipherOs = new CipherOutputStream(dataOutputStream, dataCipher);
            encOos = new ObjectOutputStream(dataCipherOs);
            encOos.writeObject(plainData);
            dataCipherOs.write(plainData);
        } finally {
            encOos.close();
        }

        // Init cipher object for encrypting the symmetric key
        PublicKey publicKey = storage.getPublicKeyForCertificate(certificateAlias);
        Cipher asymCipher = Cipher.getInstance(ASYM_ALGORITHM);
        asymCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // Encrypt the symmetric key with the proper private key
        byte[] encSymmetricKey = asymCipher.doFinal(secretKey.getEncoded());

        // Sign the plain data
        asymSign.update(plainData);

        // Create a CryptoConfiguration
        CryptoConfiguration cryptoConfig = new CryptoConfiguration();
        cryptoConfig.encSymmetricKey = encSymmetricKey;
        cryptoConfig.signature = asymSign.sign();

        // Write the CryptoConfiguration file
        writeConfigurationFile(cryptoConfig, dataFilePath + CONF_PREFIX);
    }

    private static void writeConfigurationFile(CryptoConfiguration cryptoConf, String outputFilePath) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(outputFilePath);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(cryptoConf);
        out.close();
        fileOut.close();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, SignatureException, IllegalBlockSizeException {

        // Read arguments from user
        if (args.length != 4) {
            System.out.println("Usage: Encryptor " +
                    "<data_file> <keystore_path> <keystore_password>" +
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