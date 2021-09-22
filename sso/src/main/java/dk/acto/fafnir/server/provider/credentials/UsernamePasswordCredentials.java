package dk.acto.fafnir.server.provider.credentials;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class UsernamePasswordCredentials {
    String username;
    String password;
    String organisation;
}
