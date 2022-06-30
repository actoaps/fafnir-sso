package dk.acto.fafnir.api.model;

import dk.acto.fafnir.api.model.conf.FafnirConf;

import java.util.Optional;

public class AuthenticationResult {
    private final String jwt;
    private final FailureReason failureReason;

    private AuthenticationResult(String jwt, FailureReason failureReason) {
        this.jwt = jwt;
        this.failureReason = failureReason;
    }

    public static AuthenticationResult success(String jwt) {
        return new AuthenticationResult(jwt, null);
    }

    public static AuthenticationResult failure(FailureReason failureReason){
        return new AuthenticationResult(null, failureReason);
    }

    public String getUrl (FafnirConf fafnirConf) {
        return Optional.ofNullable(jwt)
                .map(token -> fafnirConf.getSuccessRedirect() + "#" + token)
                .orElseGet(() ->fafnirConf.getFailureRedirect() +"#" +failureReason.getErrorCode());
    }
}
