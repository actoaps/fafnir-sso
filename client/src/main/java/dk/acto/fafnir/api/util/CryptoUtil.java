package dk.acto.fafnir.api.util;

import dk.acto.fafnir.api.exception.PasswordDecryptionFailed;
import dk.acto.fafnir.api.exception.PasswordEncryptionFailed;
import dk.acto.fafnir.api.exception.PasswordHashingFailed;
import dk.acto.fafnir.api.model.UserData;
import io.vavr.control.Try;
import org.bouncycastle.crypto.generators.BCrypt;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

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
                .map(x -> Base64.getEncoder().encodeToString(x))
                .getOrElseThrow(PasswordHashingFailed::new);
    }
}
