package dk.acto.fafnir.sso.provider.credentials;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UsernamePasswordCredentials {
    String username;
    String password;
    String organisation;
}
