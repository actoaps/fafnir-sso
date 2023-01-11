package dk.acto.fafnir.sso;

import dk.acto.fafnir.client.FafnirClientConfiguration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.security.Security;

@SpringBootApplication
@Profile("!test")
@Import(FafnirClientConfiguration.class)
public class Sso {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(Sso.class, args);
    }
}
