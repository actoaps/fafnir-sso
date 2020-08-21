package dk.acto.fafnir;

import dk.acto.fafnir.client.FafnirClient;
import dk.acto.fafnir.client.FafnirClientConfiguration;
import dk.acto.fafnir.client.JwtValidator;
import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.model.conf.FafnirConf;
import dk.acto.fafnir.providers.HazelcastProvider;
import dk.acto.fafnir.providers.credentials.UsernamePassword;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(FafnirClientConfiguration.class)
public class HazelcastFlowTest {
    private static final Pattern JWT_MATCHER = Pattern.compile("/success#(.+)$");

    @Autowired
    private FafnirClient fafnirClient;

    @Autowired
    private HazelcastProvider hazelcastProvider;

    @Autowired
    private FafnirConf fafnirConf;

    @Autowired
    private JwtValidator jwtValidator;
    @Test
    public void testFlow() {
        var me = FafnirUser.builder()
                .subject("om@acto.dk")
                .password("omom")
                .name("Oscar Mike")
                .provider("test")
                .build();
        fafnirClient.exportToFafnir(fafnirClient.toSecureUser(me));
        var result = hazelcastProvider.callback(UsernamePassword.builder()
                .username("om@acto.dk")
                .password("omom")
                .build());
        var url = result.getUrl(fafnirConf);
        assertThat(url).contains("/success#");
        var matcher = JWT_MATCHER.matcher(url);
        assertThat(matcher.find()).isTrue();
        var jwt = matcher.group(1);
        var auth = jwtValidator.decodeToken(jwt);
        assertThat(auth.getUsername()).isEqualTo("om@acto.dk");
        assertThat(auth.getName()).isEqualTo("Oscar Mike");



    }
}
