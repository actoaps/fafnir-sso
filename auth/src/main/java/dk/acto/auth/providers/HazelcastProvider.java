package dk.acto.auth.providers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.model.conf.FafnirConf;
import dk.acto.auth.model.conf.HazelcastConf;
import dk.acto.auth.providers.credentials.UsernamePassword;
import dk.acto.auth.services.ServiceHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean(HazelcastConf.class)
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePassword> {
    private final TokenFactory tokenFactory;
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;
    private final FafnirConf fafnirConf;

    public HazelcastProvider(TokenFactory tokenFactory,
                             @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance, HazelcastConf hazelcastConf, FafnirConf fafnirConf) {
        this.tokenFactory = tokenFactory;
        this.hazelcastInstance = hazelcastInstance;
        this.hazelcastConf = hazelcastConf;
        this.fafnirConf = fafnirConf;
    }

    @Override
    public String authenticate() {
            return "/hazelcast/login";
    }

    public String callback(final UsernamePassword data) {
        var username = data.getUsername();
        var password = data.getPassword();

        IMap<String, String> map = hazelcastInstance.getMap("fafnir-user");
        var identifier = hazelcastConf.isUsernameIsEmail() ? username.toLowerCase() : username;
        return Optional.ofNullable(map.get(identifier))
                .map(this::decryptPassword)
                .filter(password::equals)
                .map(x -> tokenFactory.generateToken(
                        FafnirUser.builder()
                                .name(identifier)
                                .subject(identifier)
                                .provider("hazelcast")
                                .build()))
                .map(x -> ServiceHelper.getJwtUrl(fafnirConf, x))
                .orElse(fafnirConf.getFailureRedirect());
    }

    private String decryptPassword(String encryptedPassword) {
        return tokenFactory.decryptString(encryptedPassword);
    }
}
