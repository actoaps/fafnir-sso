package dk.acto.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.acto.auth.providers.TestProvider;
import dk.acto.auth.providers.UniLoginConf;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static dk.acto.auth.ActoConf.DEFAULT;

@Slf4j
@Configuration
public class ActoConfFactory {
	@Bean
	public ActoConf configure(ObjectMapper objectMapper) {
		return Option.of(System.getenv("ACTO_CONF"))
				.toTry().mapTry(x -> objectMapper.readValue(x, ActoConf.class))
				.onFailure(x -> log.warn("ACTO_CONF environment variable not found, using DEFAULT"))
				.getOrElse(DEFAULT);
	}
	
	@Bean
	public UriBuilderFactory newUriBuilderFactory() {
		return new DefaultUriBuilderFactory();
	}
	
	@Bean
	public UniLoginConf newUniLoginConf(UriBuilderFactory factory, ActoConf actoConf) {
		return new UniLoginConf(factory, actoConf);
	}
	
	@Bean
	public TestProvider newTestProvider(TokenFactory tokenFactory, ActoConf actoConf) {
		return new TestProvider(tokenFactory, actoConf);
	}
}
