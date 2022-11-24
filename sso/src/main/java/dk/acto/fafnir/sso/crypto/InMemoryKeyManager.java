package dk.acto.fafnir.sso.crypto;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.crypto.X509CertificateManager;
import io.vavr.control.Try;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Value
@Component
public class InMemoryKeyManager implements RsaKeyManager, X509CertificateManager {
    RSAPublicKey publicKey;
    RSAPrivateKey privateKey;
    X509Certificate certificate;

    public InMemoryKeyManager() {
        var keys = Try.of(() -> KeyPairGenerator.getInstance("RSA"))
                .andThen(x -> x.initialize(2048, new SecureRandom()))
                .map(KeyPairGenerator::generateKeyPair)
                .get();
        publicKey = (RSAPublicKey) keys.getPublic();
        privateKey = (RSAPrivateKey) keys.getPrivate();
        certificate = CertificateUtils.createCertificate(keys);
    }
}
