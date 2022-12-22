package dk.acto.fafnir.api.service;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface CryptoService {
    /**
     * Encodes the password. Will also encrypt it if publicKey is present.
     *
     * @param password the password to be encoded.
     * @param publicKey the publicKey to be used for encrypting. It can be null.
     * @return the encoded password.
     */
    String encodePassword(String password, PublicKey publicKey);

    /**
     * Checks if the plaintext password matches the encoded one. If privateKey is present it assumes the encoded to be
     * encrypted also.
     *
     * @param password the plaintext password.
     * @param encoded the encoded password.
     * @param privateKey the privateKey to be used for decrypting. It can be null.
     * @return whether the password matches.
     */
    Boolean matches(String password, String encoded, PrivateKey privateKey);
}
