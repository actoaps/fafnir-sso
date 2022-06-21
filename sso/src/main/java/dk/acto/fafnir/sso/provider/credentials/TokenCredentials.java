package dk.acto.fafnir.sso.provider.credentials;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class TokenCredentials {
    String code;
}
