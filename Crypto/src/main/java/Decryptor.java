import crypto.CryptoVariables;
import crypto.CryptoStorage;
import crypto.Utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.*;
import java.security.cert.CertificateException;

import static crypto.CryptoConsts.*;

/**
 * Created by shmir on 1/14/2017.
 */
public class Decryptor {

    private Cipher dataDecipher;

    private CryptoStorage storage;
    private PrivateKey privateKey;

    public Decryptor(String keystorePath, String keystorePassword, String privateKeyAlias)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IOException, InvalidAlgorithmParameterException, UnrecoverableEntryException, KeyStoreException, CertificateException {

        // Initialize CryptoStorage
        storage = new CryptoStorage(keystorePath, keystorePassword);
        privateKey = storage.getPrivateKey(privateKeyAlias);

        // Init dataCipher object
        dataDecipher = Cipher.getInstance(DATA_CIPHER_GENERATOR);
    }

    public void decryptAndValidate(String encDataFilePath, String varsFilePath, String certificateAlias) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, KeyStoreException, SignatureException {

        System.out.println("Working on file: " + encDataFilePath);

        // Read decryption parameters
        System.out.println("Reading crypto variables file: " + varsFilePath);
        CryptoVariables vars = CryptoVariables.readVariablesFile(varsFilePath);
        byte[] signature = vars.signature;
        byte[] iv = vars.iv;

        // Init cipher object for decrypting the symmetric key
        SecretKey secretKey = vars.decryptKey(privateKey);
        System.out.println("Using symmetric key: " + Utils.getHexDump(secretKey.getEncoded()));

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
        System.out.println("Signature verification: " + verifies);

        System.out.println("Done!");
    }



    public static void main(String[] args) throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, BadPaddingException, SignatureException, IllegalBlockSizeException,
            ClassNotFoundException {

        // Read arguments from user
        if (args.length != 6) {
            System.out.println("Usage: Decryptor " +
                    "<encrypted_file> <crypto_vars_file> <keystore_path> <keystore_password> " +
                    "<private_key_alias> <public_key_alias>");
            return;
        }
        String encDataFilePath = args[0];
        String varsFilePath = args[1];
        String keystorePath = args[2];
        String keystorePassword = args[3];
        String privateKeyAlias = args[4];
        String certificateAlias = args[5];

        // Initialize Decryptor
        Decryptor decryptor = new Decryptor(keystorePath, keystorePassword, privateKeyAlias);

        // Decrypt the given file and validate signature
        decryptor.decryptAndValidate(encDataFilePath, varsFilePath, certificateAlias);
    }

}
