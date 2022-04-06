package dk.acto.fafnir.server.service;

import dk.acto.fafnir.server.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class)
class ProviderServiceTest {

    @Autowired
    ProviderService providerService;

    @Test
    void getAcceptedProviders() {
        var result = providerService.getAcceptedProviders();
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
        assertThat(result).contains("hazelcast");
    }

    @Test
    void getProviderInformation() {
        var result = providerService.getProviderInformation("hazelcast");
        assertThat(result).isNotNull();
    }
}
