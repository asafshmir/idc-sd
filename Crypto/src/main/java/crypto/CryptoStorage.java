package crypto;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.*;
import java.security.cert.CertificateException;

import static crypto.CryptoConsts.*;

/**
 * Represents a secure keystore file holding a private key and trusted certificates.
 */
public class CryptoStorage {

    /** Represents the keystore file */
    private KeyStore keyStore;
    /** The password used both for protecting the keystore file and the keys inside **/
    private String password;

    /**
     * Construct a {@link CryptoStorage} object
     * @param keystorePath the path of the keystore file
     * @param keystorePassword the password protecting the file and the private key
     */
    public CryptoStorage(String keystorePath, String keystorePassword)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
        FileInputStream ksFis = new FileInputStream(keystorePath);
        password = keystorePassword;
        keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(ksFis, keystorePassword.toCharArray());
    }

    /**
     * Return the {@link PrivateKey} that is securely stores in the keystore
     * @param privateKeyAlias the alias for the certificate
     * @return the {@link PrivateKey}
     */
    public PrivateKey getPrivateKey(String privateKeyAlias) throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)
                keyStore.getEntry(privateKeyAlias, new KeyStore.PasswordProtection(password.toCharArray()));
        return keyEntry.getPrivateKey();
    }

    /**
     * Return the {@link PublicKey} of the given alias
     * @param certificateAlias the alias of whom the {@link PublicKey} belongs
     * @return the {@link PublicKey} of the given alias
     */
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