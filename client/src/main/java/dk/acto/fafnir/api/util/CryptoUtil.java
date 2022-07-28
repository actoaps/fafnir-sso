package dk.acto.fafnir.api.util;

import dk.acto.fafnir.api.exception.InvalidPublicKey;
import dk.acto.fafnir.api.exception.PasswordDecryptionFailed;
import dk.acto.fafnir.api.exception.PasswordEncryptionFailed;
import dk.acto.fafnir.api.exception.PasswordHashingFailed;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import io.vavr.control.Try;
import org.bouncycastle.crypto.generators.BCrypt;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

public final class CryptoUtil {
    private CryptoUtil() {
    }

    public static String decryptPassword(String encrypted, PrivateKey privateKey) {
        return Try.of(() -> Cipher.getInstance("RSA/None/OAEPWITHSHA-256ANDMGF1PADDING"))
                .andThenTry(x -> x.init(Cipher.DECRYPT_MODE, privateKey))
                .mapTry(x -> x.doFinal(Base64.getDecoder().decode(encrypted)))
                .map(x -> new String(x, StandardCharsets.UTF_8))
                .getOrElseThrow(PasswordDecryptionFailed::new);
    }

    public static String encryptPassword(String password, PublicKey publicKey) {
        return Try.of(() -> Cipher.getInstance("RSA/None/OAEPWITHSHA-256ANDMGF1PADDING"))
                .andThenTry(x -> x.init(Cipher.ENCRYPT_MODE, publicKey))
                .mapTry(x -> x.doFinal(password.getBytes(StandardCharsets.UTF_8)))
                .map(x -> Base64.getEncoder().encodeToString(x))
                .getOrElseThrow(PasswordEncryptionFailed::new);
    }

    public static String hashPassword(String password) {
        return Try.of(() -> BCrypt.passwordToByteArray(password.toCharArray()))
                .map(Base64.getEncoder()::encodeToString)
                .getOrElseThrow(PasswordHashingFailed::new);
    }

    public static PublicKey toPublicKey(PublicKeyProvider provider) {
        return Optional.of(provider.getPublicKey())
                .map(Base64.getDecoder()::decode)
                .map(X509EncodedKeySpec::new)
                .map(x -> Try.of(() -> KeyFactory.getInstance("RSA"))
                        .mapTry(y -> y.generatePublic(x))
                        .toJavaOptional()
                        .orElseThrow(InvalidPublicKey::new))
                .orElseThrow(InvalidPublicKey::new);
    }
}
