package dk.acto.fafnir.api.crypto;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public interface RsaKeyManager {
    RSAPrivateKey getPrivateKey();
    RSAPublicKey getPublicKey();
}
