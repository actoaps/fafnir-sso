package dk.acto.fafnir.api.model;

import dk.acto.fafnir.api.util.CryptoUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String subject;
    String password;
    String provider;
    String name;
    String metaId;
    Locale locale;
    Instant created;

    public UserData secure(final PublicKey publicKey) {
        return this.toBuilder()
                        .password(
        Optional.ofNullable(publicKey)
                .map(pk -> CryptoUtil.encryptPassword(password, pk))
                .orElse(CryptoUtil.hashPassword(this.getPassword())))
                .build();
    }

    public boolean canAuthenticate(final String password, final PrivateKey privateKey) {
        return Optional.ofNullable(privateKey)
                .map(pk -> authCrypt(password, pk))
                .orElse(authHash(password));
    }

    private boolean authHash(final String password) {
        return CryptoUtil.hashPassword(password).equals(this.password);
    }

    private boolean authCrypt(final String password, PrivateKey privateKey) {
        return password.equals(CryptoUtil.decryptPassword(this.password, privateKey));
    }
}
