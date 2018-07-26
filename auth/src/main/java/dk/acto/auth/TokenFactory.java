package dk.acto.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.io.BaseEncoding;
import io.vavr.control.Try;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.util.Date;

public class TokenFactory {

    private final KeyPair keys;

    public TokenFactory() {
        this.keys = Try.of(() -> KeyPairGenerator.getInstance("RSA"))
                .andThen(x -> x.initialize(1024, new SecureRandom()))
                .map(KeyPairGenerator::generateKeyPair)
                .get();
    }

    public String generateToken(String subject) {
        return Try.of(() -> Algorithm.RSA512(RSAPublicKey.class.cast(keys.getPublic()), RSAPrivateKey.class.cast(keys.getPrivate())))
                .map(x -> JWT.create()
                        .withIssuer("acto")
                        .withSubject(subject)
                        .withIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                        .sign(x))
                .get();
    }

    public String getPublicKey() {
        return BaseEncoding.base64().omitPadding().encode(
                keys.getPublic().getEncoded()
        );
    }
}
