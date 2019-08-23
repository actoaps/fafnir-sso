package dk.acto.auth;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@Log4j2
public class Main {
	private final TokenFactory tokenFactory;
	private final ActoConf actoConf;

	public Main(TokenFactory tokenFactory, ActoConf actoConf) {
		this.tokenFactory = tokenFactory;
		this.actoConf = actoConf;
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.actoConf.isTestMode()) {
			String jwt = tokenFactory.generateToken("test", "test", "Testy McTestface");
			log.info("Test token: " + jwt);
		}
	}
}
