package dk.acto.fafnir.api.model;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class AuthenticationResult {
    private final String jwt;
    private final FailureReason failureReason;
    private final String redirectUrl;

    public static AuthenticationResult success(String jwt) {
        return new AuthenticationResult(jwt, null, null);
    }

    public static AuthenticationResult failure(FailureReason failureReason) {
        return new AuthenticationResult(null, failureReason, null);
    }

    public static AuthenticationResult redirect(String redirectUrl) {
        return new AuthenticationResult(null, null, redirectUrl);
    }

    public String getUrl(FafnirConf fafnirConf) {
        if (redirectUrl != null) {
            return redirectUrl;
        } else {
            return Optional.ofNullable(jwt)
                .map(token -> fafnirConf.getSuccessRedirect() + "#" + token)
                .orElseGet(() -> fafnirConf.getFailureRedirect() + "#" + (failureReason != null ? failureReason.getErrorCode() : ""));
        }
    }
}


