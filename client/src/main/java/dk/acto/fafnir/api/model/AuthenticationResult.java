package dk.acto.fafnir.api.model;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class AuthenticationResult {
    private final String jwt;
    private final FailureReason failureReason;

    public static AuthenticationResult success(String jwt) {
        return new AuthenticationResult(jwt, null);
    }

    public static AuthenticationResult failure(FailureReason failureReason) {
        return new AuthenticationResult(null, failureReason);
    }

    public String getUrl(FafnirConf fafnirConf) {
        return Optional.ofNullable(jwt)
                .map(token -> fafnirConf.getSuccessRedirect() + "#" + token)
                .orElseGet(() -> fafnirConf.getFailureRedirect() + "#" + failureReason.getErrorCode());
    }
}
