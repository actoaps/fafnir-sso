package dk.acto.fafnir.iam.security.conf;

import dk.acto.fafnir.client.JwtValidator;
import dk.acto.fafnir.client.providers.AuthoritiesProvider;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import io.vavr.control.Try;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class FafnirConfiguration {

    @Bean
    public AuthoritiesProvider authoritiesProvider() {
        return claims -> Optional.ofNullable(claims.get("role"))
                .map(x -> Try.of(() -> (List<String>) x).getOrElse(List.of()))
                .map(x -> x.stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Bean
    public JwtValidator jwtValidator(PublicKeyProvider publicKeyProvider, AuthoritiesProvider authoritiesProvider) {
        return new JwtValidator(publicKeyProvider, authoritiesProvider);
    }

}
