package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FailureReason {
    AUTHENTICATION_FAILED(400),
    CONNECTION_FAILED(500);

    @Getter
    private final int errorCode;

}

