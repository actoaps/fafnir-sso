package dk.acto.fafnir.iam;

import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.security.Security;

@SpringBootApplication
@Profile("!test")
public class Iam {

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        MetadataProvider.getAllSupportedProviders();
        SpringApplication.run(Iam.class, args);
    }
}
