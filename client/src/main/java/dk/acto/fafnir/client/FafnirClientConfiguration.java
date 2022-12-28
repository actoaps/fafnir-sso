package dk.acto.fafnir.client;

import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.CryptoService;
import dk.acto.fafnir.api.service.hazelcast.CryptoServiceImpl;
import dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.client.providers.builtin.RestfulPublicKeyProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Component
@ComponentScan
public class FafnirClientConfiguration {

    @Bean
    public HazelcastConf hazelcastConf(
            @Value("${HAZELCAST_MAP_NAME:fafnir-users}") String mapName,
            @Value("${HAZELCAST_USERNAME_IS_EMAIL:false}") boolean userNameIsEmail,
            @Value("${HAZELCAST_PASSWORD_IS_ENCRYPTED:false}") boolean passwordIsEncrypted,
            @Value("${HAZELCAST_TRIM_USERNAME:false}") boolean trimUsername) {
        return new HazelcastConf(userNameIsEmail, passwordIsEncrypted, trimUsername, mapName);
    }

    @Bean
    public CryptoService cryptoService() {
        return new CryptoServiceImpl();
    }

    @Bean
    public AdministrationService administrationService(HazelcastInstance hazelcastInstance, HazelcastConf hazelcastConf,
                                                       PublicKeyProvider publicKeyProvider, CryptoService cryptoService) {
        return new HazelcastAdministrationService(hazelcastInstance, hazelcastConf, publicKeyProvider, cryptoService);
    }

}
