package dk.acto.fafnir.iam.conf;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class BeanConf {

    @Bean
    @Primary
    public HazelcastConf hazelcastConf(
            @Value("${HAZELCAST_MAP_NAME:fafnir-users}") String mapName,
            @Value("${HAZELCAST_USERNAME_IS_EMAIL:false}") boolean userNameIsEmail,
            @Value("${HAZELCAST_PASSWORD_IS_ENCRYPTED:false}") boolean passwordIsEncrypted,
            @Value("${HAZELCAST_TRIM_USERNAME:false}") boolean trimUsername) {
        return new HazelcastConf(userNameIsEmail, passwordIsEncrypted, trimUsername, mapName);
    }

    @Bean
    @ConditionalOnProperty(name = "HAZELCAST_TCP_IP_ADDRESS")
    public ClientConfig hazelcastInstanceConf (@Value("${HAZELCAST_TCP_IP_ADDRESS}") String address) {
        log.info("Hazelcast TCP/IP Connection Configured...");
        var config = new ClientConfig();
        config.getNetworkConfig().addAddress(address);
        return config;
    }

    @Bean
    public AdministrationService administrationService(HazelcastInstance hazelcastInstance, HazelcastConf hazelcastConf) {
        return new HazelcastAdministrationService(hazelcastInstance, hazelcastConf);
    }
}
