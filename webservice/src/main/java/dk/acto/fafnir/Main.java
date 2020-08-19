package dk.acto.fafnir;

import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.model.conf.TestConf;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.Optional;

@SpringBootApplication
@AllArgsConstructor
@Slf4j
public class Main {
    private final Optional<TestConf> testConf;
    private final TokenFactory tokenFactory;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (testConf.isPresent()) {
            String jwt = tokenFactory.generateToken(FafnirUser.builder()
                    .subject("test")
                    .provider("test")
                    .name("Testy McTestFace")
                    .build());
            log.info("Test token: " + jwt);
        }
    }

}
