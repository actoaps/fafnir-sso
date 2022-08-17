package dk.acto.fafnir.iam.security.conf;

import dk.acto.fafnir.client.JwtFilter;
import dk.acto.fafnir.client.JwtValidator;
import dk.acto.fafnir.client.providers.AuthoritiesProvider;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

@Configuration
public class FafnirConfiguration {

    @Bean
    public AuthoritiesProvider authoritiesProvider() {
        return claims -> Optional.ofNullable(claims.get("sub"))
                .map(String::valueOf)
                .map(x -> List.of((GrantedAuthority) new SimpleGrantedAuthority(x)))
                .orElse(List.of());
    }

    @Bean
    public JwtValidator jwtValidator(PublicKeyProvider publicKeyProvider, AuthoritiesProvider authoritiesProvider) {
        return new JwtValidator(publicKeyProvider, authoritiesProvider);
    }

    @Bean
    public JwtFilter jwtFilter(JwtValidator jwtValidator) {
        return new JwtFilter(jwtValidator);
    }

}
