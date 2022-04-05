package dk.acto.fafnir.server;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.security.Security;

@SpringBootApplication
@AllArgsConstructor
@Slf4j
@Profile("demo")
public class Main {
    private final DemoDataGenerator demoDataGenerator;

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner( ) {
        return args -> {
            // User Dummy Data
            demoDataGenerator.createDemoData();

        };
    }
}
