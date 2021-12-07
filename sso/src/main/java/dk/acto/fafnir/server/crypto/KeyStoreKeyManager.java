package dk.acto.fafnir.server.crypto;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.exception.CouldNotGenerateCertificate;
import dk.acto.fafnir.api.exception.CouldNotLoadKeyStore;
import dk.acto.fafnir.api.exception.NoSuchSignatureAlgorithm;
import io.vavr.control.Try;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Data
@Component
@Primary
@ConditionalOnProperty(name = {"KEYSTORE_PASS", "KEY_PASS"})
public class KeyStoreKeyManager implements RsaKeyManager {
    private static final String KEYSTORE_FILENAME = Files.exists(Path.of("/var/lib/fafnir/")) ? "/var/lib/fafnir/fafnir.jks;" : "./fafnir.jks";
    private static final String KEY_ALIAS = "FAFNIR";
    private final X509Certificate certificate;
    private final RSAPrivateKey privateKey;

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
                .andThen(x -> x.initialize(1024, new SecureRandom()))
                .map(KeyPairGenerator::generateKeyPair)
                .get();
        var cert = createCertificate(keyPair);

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

    private X509Certificate createCertificate(KeyPair keyPair) {
        // Prepare the information required for generating an X.509 certificate.
        X500Name owner = new X500Name("CN=Fafnir Server");
        var signer = Try.of(() -> new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .build(keyPair.getPrivate()))
                .getOrElseThrow(NoSuchSignatureAlgorithm::new);
        return Try.of(() -> new JcaX509v3CertificateBuilder(
                owner,
                new BigInteger(64, new SecureRandom()),
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(1000, ChronoUnit.DAYS)),
                owner,
                keyPair.getPublic()))
                .map(builder -> builder.build(signer))
                .mapTry(holder -> new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder))
                .onFailure(x -> log.error("Error:", x))
                .getOrElseThrow(CouldNotGenerateCertificate::new);
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) certificate.getPublicKey();
    }
}
