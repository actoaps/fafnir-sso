package dk.acto.auth.model;

import dk.acto.auth.FailureReason;

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
}
