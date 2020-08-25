package dk.acto.fafnir.model;

import dk.acto.fafnir.FailureReason;
import dk.acto.fafnir.model.conf.FafnirConf;

import java.util.Optional;

public class CallbackResult {
    private final String jwt;
    private final FailureReason failureReason;

    private CallbackResult(String jwt, FailureReason failureReason) {
        this.jwt = jwt;
        this.failureReason = failureReason;
    }

    public static CallbackResult success(String jwt) {
        return new CallbackResult(jwt, null);
    }

    public static CallbackResult failure(FailureReason failureReason){
        return new CallbackResult(null, failureReason);
    }

    public String getUrl (FafnirConf fafnirConf) {
        return Optional.ofNullable(jwt)
                .map(token -> fafnirConf.getSuccessRedirect() + "#" + token)
                .orElseGet(() ->fafnirConf.getFailureRedirect() +"#" +failureReason.getErrorCode());
    }
}
