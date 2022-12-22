package dk.acto.fafnir.sso.dto;

import dk.acto.fafnir.api.model.FailureReason;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginResponseInfo {
    String jwt;
    FailureReason error;
}
