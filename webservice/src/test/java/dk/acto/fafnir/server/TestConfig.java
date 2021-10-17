package dk.acto.fafnir.server;

import dk.acto.fafnir.client.providers.AuthoritiesProvider;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TestConfig {
    @Bean
    public AuthoritiesProvider authoritiesProvider() {
        return claims -> List.of();
    }

    @Bean
    public PublicKeyProvider publicKeyProvider(TokenFactory tokenFactory) {
        return tokenFactory::getPublicKey;
    }

}
