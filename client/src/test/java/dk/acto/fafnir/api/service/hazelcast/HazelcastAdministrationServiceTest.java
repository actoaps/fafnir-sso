package dk.acto.fafnir.api.service.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HazelcastAdministrationServiceTest {

    private final HazelcastAdministrationService subject = getService();

    @Test
    void createAndReadUser() {
        var ab = UserData.builder()
                .subject("ALPHA_BRAVO")
                .password("abob")
                .name("Alpha Bravo")
                .build();
        var result = subject.createUser(ab);
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getSubject()).isNotNull();
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getName()).isNotNull();

        var readResult = subject.readUser(ab.getSubject());
        assertThat(result).isEqualTo(readResult);
    }

    @Test
    void readUsers() {
        var bc = UserData.builder()
                .subject("ALPHA_BRAVO")
                .password("abob")
                .name("Alpha Bravo")
                .build();
        var cd = UserData.builder()
                .subject("ALPHA_BRAVO")
                .password("abob")
                .name("Alpha Bravo")
                .build();

        var result = Stream.of(
                subject.createUser(bc),
                subject.createUser(cd))
                .toArray(UserData[]::new);

        assertThat(subject.readUsers()).contains(result);
        assertThat(subject.readUsers(0L).getPageData()).contains(result);
    }

    @Test
    void updateUser() {
    }

    @Test
    void deleteUser() {
    }

    @Test
    void createOrganisation() {
    }

    @Test
    void readOrganisation() {
    }

    @Test
    void testReadOrganisation() {
    }

    @Test
    void testReadOrganisation1() {
    }

    @Test
    void updateOrganisation() {
    }

    @Test
    void deleteOrganisation() {
    }

    @Test
    void createClaim() {
    }

    @Test
    void readClaims() {
    }

    @Test
    void testReadClaims() {
    }

    @Test
    void updateClaims() {
    }

    @Test
    void deleteClaims() {
    }

    @Test
    void getOrganisationsForUser() {
    }

    @Test
    void getUsersForOrganisation() {
    }

    @Test
    void readOrganisations() {
    }

    @Test
    void testReadOrganisations() {
    }

    @Test
    void countOrganisations() {
    }

    private static HazelcastAdministrationService getService() {
        Config config = new Config();
        config.setProperty("hazelcast.shutdownhook.enabled", "false");
        NetworkConfig network = config.getNetworkConfig();
        network.getInterfaces().setEnabled(false);
        var instance = Hazelcast.newHazelcastInstance(config);

        return new HazelcastAdministrationService(
                        instance,
                        new HazelcastConf(true,
                                false,
                                true,
                                "TEST"
                        ));

    }
}
