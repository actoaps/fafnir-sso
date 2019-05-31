package dk.acto.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.acto.auth.providers.TestProvider;
import dk.acto.auth.providers.UniLoginConf;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dk.acto.auth.ActoConf.DEFAULT;

@Slf4j
@Configuration
public class ActoConfFactory {
	@Bean
	public ActoConf getActoConf(@Value("${ACTO_CONF}") String actoConfJson, ObjectMapper objectMapper) {
		return Option.of(actoConfJson)
				.toTry().mapTry(x -> objectMapper.readValue(x, ActoConf.class))
				.onFailure(x -> log.warn("ACTO_CONF environment variable not found, using DEFAULT", x))
				.getOrElse(DEFAULT);
	}
	
	@Bean
	public UniLoginConf newUniLoginConf(ActoConf actoConf) {
		return new UniLoginConf(actoConf);
	}
	
	@Bean
	public TestProvider newTestProvider(TokenFactory tokenFactory, ActoConf actoConf) {
		return new TestProvider(tokenFactory, actoConf);
	}
}
