package dk.acto.fafnir.server.crypto;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import io.vavr.control.Try;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Value
@Component
public class InMemoryKeyManager implements RsaKeyManager {
    RSAPublicKey publicKey;
    RSAPrivateKey privateKey;

    public InMemoryKeyManager() {
        var keys = Try.of(() -> KeyPairGenerator.getInstance("RSA"))
                .andThen(x -> x.initialize(1024, new SecureRandom()))
                .map(KeyPairGenerator::generateKeyPair)
                .get();
        publicKey = (RSAPublicKey) keys.getPublic();
        privateKey = (RSAPrivateKey) keys.getPrivate();
    }
}
