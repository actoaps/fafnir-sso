package dk.acto.fafnir;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import static dk.acto.fafnir.ActoConf.DEFAULT;

@Slf4j
//@Configuration
public class ActoConfFactory {
//	@Bean
	public ActoConf getActoConf(@Value("${ACTO_CONF}") String actoConfJson, ObjectMapper objectMapper) {
		return Option.of(actoConfJson)
				.toTry().mapTry(x -> objectMapper.readValue(x, ActoConf.class))
				.onFailure(x -> log.warn("ACTO_CONF environment variable not found, using DEFAULT", x))
				.getOrElse(DEFAULT);
	}
}
