package dk.acto.fafnir.sso.provider.unilogin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class UniloginTokenCredentials {
    String code;
    String state;
}
