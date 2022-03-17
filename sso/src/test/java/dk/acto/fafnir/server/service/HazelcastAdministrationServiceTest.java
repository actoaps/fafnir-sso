package dk.acto.fafnir.server.service;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.server.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class)
class HazelcastAdministrationServiceTest {
    @Autowired
    private HazelcastAdministrationService administrationService;

    @Autowired
    private RsaKeyManager rsaKeyManager;

    @Test
    void testCreateUser() {
        var result = administrationService.createUser(UserData.builder()
                        .provider("test")
                        .metaId("meta")
                        .name("Alpha Bravo")
                        .password("abob")
                        .subject("ab@Acto.dk")
                .build().secure(rsaKeyManager.getPublicKey()));

        assertThat(result).isNotNull();
        assertThat(administrationService.readUsers().length).isGreaterThan(0);
    }
}
