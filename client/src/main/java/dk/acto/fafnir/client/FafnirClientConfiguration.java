package dk.acto.fafnir.client;

import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class FafnirClientConfiguration {

    @Bean
    public FafnirClient fafnirClient(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance,
                                     PublicKeyProvider publicKeyProvider, HazelcastConf hazelcastConf) {
        return new FafnirClient(hazelcastInstance, publicKeyProvider, hazelcastConf);
    }

    @Bean
    public HazelcastConf hazelcastConf(
            @Value("${HAZELCAST_MAP_NAME:fafnir-users}") String mapName,
            @Value("${HAZELCAST_USERNAME_IS_EMAIL:false}") boolean userNameIsEmail,
            @Value("${HAZELCAST_PASSWORD_IS_ENCRYPTED:false}") boolean passwordIsEncrypted),
            @Value("${HAZELCAST_TRIM_USERNAME:false}") boolean trimUsername) {
        return new HazelcastConf(userNameIsEmail, passwordIsEncrypted, trimUsername, mapName);
    }
}
