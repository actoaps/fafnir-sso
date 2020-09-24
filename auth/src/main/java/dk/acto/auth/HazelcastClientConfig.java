package dk.acto.auth;

import com.hazelcast.client.config.ClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastClientConfig {
    @Bean
    public ClientConfig eyy () {
        var config = new ClientConfig();
        config.getNetworkConfig().addAddress("hazelcast");
        return config;
    }
}
