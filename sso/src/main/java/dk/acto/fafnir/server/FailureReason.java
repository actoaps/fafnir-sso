package dk.acto.fafnir.server;

public enum FailureReason {
    AUTHENTICATION_FAILED(400),
    CONNECTION_FAILED(500);

    private final int errorCode;

    FailureReason(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}

