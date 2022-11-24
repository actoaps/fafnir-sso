package dk.acto.fafnir.sso.crypto;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.crypto.X509CertificateManager;
import dk.acto.fafnir.api.exception.CouldNotLoadKeyStore;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@lombok.Value
@Component
@Primary
@ConditionalOnProperty(name = {"KEYSTORE_PASS", "KEY_PASS"})
public class KeyStoreKeyManager implements RsaKeyManager, X509CertificateManager {
    private static final String KEYSTORE_FILENAME = Files.exists(Path.of("/var/lib/fafnir/")) ? "/var/lib/fafnir/fafnir.jks" : "./fafnir.jks";
    private static final String KEY_ALIAS = "FAFNIR";
    X509Certificate certificate;
    RSAPrivateKey privateKey;

    public KeyStoreKeyManager(@Value("${KEYSTORE_PASS}") String keyStorePassword, @Value("${KEY_PASS}") String keyPassword) {
        var keyStore = Try.withResources(() -> new FileInputStream(KEYSTORE_FILENAME))
                .of(is -> {
                    var ks = KeyStore.getInstance("jks");
                    ks.load(is, keyStorePassword.toCharArray());
                    return ks;
                })
                .getOrElseGet(x -> createKeyStore(keyStorePassword, keyPassword));

        privateKey = (RSAPrivateKey) Try.of(() -> keyStore.getKey(KEY_ALIAS, keyPassword.toCharArray()))
                .getOrElseThrow(CouldNotLoadKeyStore::new);

        certificate = (X509Certificate) Try.of(() -> keyStore.getCertificate(KEY_ALIAS))
                .getOrElseThrow(CouldNotLoadKeyStore::new);
    }

    private KeyStore createKeyStore(String keyStorePassword, String keyPassword) {
        var keyPair = Try.of(() -> KeyPairGenerator.getInstance("RSA"))
                .andThen(x -> x.initialize(2048, new SecureRandom()))
                .map(KeyPairGenerator::generateKeyPair)
                .get();
        var cert = CertificateUtils.createCertificate(keyPair);

        var keystore = Try.of(() -> KeyStore.getInstance("jks"))
                .andThenTry(x -> x.load(null))
                .andThenTry(x -> x.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[]{cert}))
                .get();

        return Try.withResources(() -> new FileOutputStream(KEYSTORE_FILENAME))
                .of(out ->
                {
                    keystore.store(out, keyStorePassword.toCharArray());
                    return keystore;
                }).get();
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) certificate.getPublicKey();
    }
}
