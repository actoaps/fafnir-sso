package dk.acto.auth.providers.credentials;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class UsernamePassword {
    String username;
    String password;
}
