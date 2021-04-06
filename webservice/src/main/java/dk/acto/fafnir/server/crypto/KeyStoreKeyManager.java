package dk.acto.fafnir.server.crypto;

import dk.acto.fafnir.api.exception.CouldNotGenerateCertificate;
import dk.acto.fafnir.api.exception.CouldNotLoadKeyStore;
import dk.acto.fafnir.api.exception.NoSuchSignatureAlgorithm;
import io.vavr.control.Try;
import lombok.Value;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
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

@Value
@Component
@ConditionalOnProperty(name = {"KEYSTORE_PASS", "KEY_PASS"})
public class KeyStoreKeyManager implements RsaKeyManager {
    X509Certificate certificate;
    RSAPrivateKey privateKey;
    String keyStorePassword;
    String keyPassword;

    public KeyStoreKeyManager(String keyStorePassword, String keyPassword) {
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        var keyStore = Try.withResources(() -> new FileInputStream("fafnir.jks"))
                .of(is -> {
                    var ks = KeyStore.getInstance("jks");
                    ks.load(is, "password".toCharArray());
                    return ks;
                }).getOrElse(createKeyStore());

            privateKey = (RSAPrivateKey) Try.of(() -> keyStore.getKey("FAFNIR", this.keyPassword.toCharArray()))
                    .getOrElseThrow(CouldNotLoadKeyStore::new);

            certificate = (X509Certificate) Try.of(() ->keyStore.getCertificate("FAFNIR"));
        })




    }

    private KeyStore createKeyStore() {
        var keyPair = Try.of(() -> KeyPairGenerator.getInstance("RSA"))
                .andThen(x -> x.initialize(1024, new SecureRandom()))
                .map(KeyPairGenerator::generateKeyPair)
                .get();
        var cert = createCertificate(keyPair);

        var keystore = Try.of(() -> KeyStore.getInstance("jks"))
                .andThenTry(x -> x.load(null))
                .andThenTry(x -> x.setKeyEntry("FAFNIR", privateKey, keyPassword.toCharArray(), new Certificate[]{certificate}))
                .get();

        return Try.withResources(() -> new FileOutputStream("fafnir.jks"))
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
                Date.from(Instant.now().plus(10, ChronoUnit.YEARS)),
                owner,
                keyPair.getPublic()))
                .map(builder -> builder.build(signer))
                .mapTry(holder -> new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder))
                .getOrElseThrow(CouldNotGenerateCertificate::new);
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) certificate.getPublicKey();
    }
}
