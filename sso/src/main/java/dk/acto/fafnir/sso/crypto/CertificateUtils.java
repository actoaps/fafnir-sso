package dk.acto.fafnir.sso.crypto;

import dk.acto.fafnir.api.exception.CouldNotGenerateCertificate;
import dk.acto.fafnir.api.exception.NoSuchSignatureAlgorithm;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
public class CertificateUtils {
    public static X509Certificate createCertificate(KeyPair keyPair) {
        var owner = new X500Name("CN=Fafnir Server");
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
}
