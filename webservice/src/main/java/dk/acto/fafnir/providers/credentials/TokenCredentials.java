package dk.acto.fafnir.providers.credentials;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class TokenCredentials {
    String token;
}
