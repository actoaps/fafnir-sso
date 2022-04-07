package dk.acto.fafnir.server.service;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService;
import dk.acto.fafnir.server.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigInteger;
import java.util.List;

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
                        .metaId("meta")
                        .name("Alpha Bravo")
                        .password("abob")
                        .subject("ab@acto.dk")
                .build().secure(rsaKeyManager.getPublicKey()));

        assertThat(result).isNotNull();
        assertThat(administrationService.readUsers(0L).getTotalPages()).isGreaterThan(BigInteger.ZERO);
        assertThat(administrationService.readUsers(0L).getPageData()).contains(result);
    }

    @Test
    void testCreateOrganisation() {
        var result = administrationService.createOrganisation(OrganisationData.builder()
                        .organisationName("Acto ApS")
                        .organisationId("acto-aps")
                .build());
        assertThat(result).isNotNull();
        assertThat(administrationService.readOrganisations().length).isGreaterThan(0);
        assertThat(administrationService.readOrganisations()).contains(result);
    }

    @Test
    void testCreateClaim() {
        var result = administrationService.createClaim(ClaimData.builder()
                        .subject("cd@acto.dk")
                        .organisationId("acto")
                        .claims(List.of("Ninja").toArray(String[]::new))
                .build());
        assertThat(result).isNotNull();
        assertThat(administrationService.readClaims("acto", "cd@acto.dk")).isNotNull();
        assertThat(administrationService.readClaims("acto", "cd@acto.dk").getClaims()).contains("Ninja");
    }

}
