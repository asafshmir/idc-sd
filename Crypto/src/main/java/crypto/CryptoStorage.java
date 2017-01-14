package crypto;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.*;
import java.security.cert.CertificateException;

import static crypto.CryptoConsts.*;

/**
 * Created by shmir on 1/14/2017.
 */
public class CryptoStorage {

    private KeyStore keyStore;



    /** The password used both for protecting the keystore file and the keys inside **/
    private String password;

    public CryptoStorage(String keystorePath, String keystorePassword)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
        FileInputStream ksFis = new FileInputStream(keystorePath);
        password = keystorePassword;
        keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(ksFis, keystorePassword.toCharArray());
    }

    public PrivateKey getPrivateKey(String privateKeyAlias) throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)
                keyStore.getEntry(privateKeyAlias, new KeyStore.PasswordProtection(password.toCharArray()));
        return keyEntry.getPrivateKey();
    }

    public PublicKey getPublicKeyForCertificate(String certificateAlias) throws NoSuchAlgorithmException, KeyStoreException {

        // Get certificate for given alias
        Certificate cert = keyStore.getCertificate(certificateAlias);

        // Get public key for certificate
        PublicKey publicKey = null;
        if (cert != null) {
            publicKey = cert.getPublicKey();
        }

        return publicKey;
    }
}