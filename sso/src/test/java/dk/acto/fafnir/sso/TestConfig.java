package dk.acto.fafnir.sso;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.client.JwtValidator;
import dk.acto.fafnir.client.providers.AuthoritiesProvider;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.client.providers.builtin.RsaKeyMangerPublicKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.security.Security;
import java.util.List;

@Configuration
@SpringBootApplication
public class TestConfig {

    @PostConstruct
    public void addBCProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    public AuthoritiesProvider authoritiesProvider() {
        return claims -> List.of();
    }

    @Bean
    public PublicKeyProvider publicKeyProvider(RsaKeyManager keyManager) {
        return new RsaKeyMangerPublicKeyProvider(keyManager);
    }

    @Bean
    public JwtValidator jwtValidator(PublicKeyProvider publicKeyProvider, AuthoritiesProvider authoritiesProvider ) {
        return new JwtValidator(publicKeyProvider, authoritiesProvider);
    }
}
