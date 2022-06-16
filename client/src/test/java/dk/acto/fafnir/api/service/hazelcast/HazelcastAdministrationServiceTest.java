package dk.acto.fafnir.api.service.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                .subject("BRAVO_CHARLIE")
                .password("bobcoc")
                .name("Bravo Charlie")
                .build();
        var cd = UserData.builder()
                .subject("CHARLIE_DELTA")
                .password("cocdod")
                .name("Charlie Delta")
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
        var de = UserData.builder()
                .subject("DELTA_ECHO")
                .password("dode")
                .name("Delta Echo")
                .build();
        var temp = subject.createUser(de);
        assertThat(temp).isNotNull();
        var result = subject.updateUser(UserData.builder()
                .subject("DELTA_ECHO")
                .name("Echo Delta")
                .build());
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isEqualTo(temp.getCreated());
        assertThat(result.getSubject()).isEqualTo(temp.getSubject());
        assertThat(result.getPassword()).isEqualTo(temp.getPassword());
        assertThat(result.getName()).isNotEqualTo(temp.getName());
        var secondResult = subject.readUser(result.getSubject());
        assertThat(secondResult).isEqualTo(result);
    }

    @Test
    void deleteUser() {
        var ef = UserData.builder()
                .subject("ECHO_FOXTROT")
                .password("efof")
                .name("Echo Foxtrot")
                .build();
        var temp = subject.createUser(ef);
        assertThat(temp).isNotNull();
        var result = subject.deleteUser(ef.getSubject());
        assertThat(temp).isEqualTo(result);
        assertThat(subject.readUsers()).doesNotContain(result);
    }

    @Test
    void createAndReadOrganisation() {
        var php25 = OrganisationData.builder()
                .organisationId("PHP_25")
                .organisationName("PHP 25")
                .build();
        var result = subject.createOrganisation(php25);
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getOrganisationId()).isNotNull();
        assertThat(result.getOrganisationName()).isNotNull();
        var readResult = subject.readOrganisation(php25.getOrganisationId());
        assertThat(readResult).isEqualTo(result);
    }


    @Test
    void readOrganisations() {
    }


    @Test
    void updateOrganisation() {
    }

    @Test
    void deleteOrganisation() {
    }

    @Test
    void createAndReadClaim() {
    }

    @Test
    void readClaims() {
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

    private static HazelcastAdministrationService getService() {
        Config config = new Config();
//        config.setProperty("hazelcast.shutdownhook.enabled", "false");
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
