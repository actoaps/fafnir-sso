package dk.acto.auth.providers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.providers.credentials.UsernamePassword;
import dk.acto.auth.services.ServiceHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePassword> {
    private final TokenFactory tokenFactory;
    private final ActoConf actoConf;
    private final HazelcastInstance hazelcastInstance;

    public HazelcastProvider(TokenFactory tokenFactory,
                             ActoConf actoConf,
                             @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.tokenFactory = tokenFactory;
        this.actoConf = actoConf;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public String authenticate() {
            return "/hazelcast/login";
    }

    public String callback(final UsernamePassword data) {
        var username = data.getUsername();
        var password = data.getPassword();

        IMap<String, String> map = hazelcastInstance.getMap("fafnir-user");
        var identifier = actoConf.isHazelcastUsernameIsEmail() ? username.toLowerCase() : username;
        return Optional.ofNullable(map.get(identifier))
                .map(this::decryptPassword)
                .filter(password::equals)
                .map(x -> tokenFactory.generateToken(
                        FafnirUser.builder()
                                .name(identifier)
                                .subject(identifier)
                                .provider("hazelcast")
                                .build()))
                .map(x -> ServiceHelper.getJwtUrl(actoConf, x))
                .orElse(actoConf.getFailureUrl());
    }

    private String decryptPassword(String encryptedPassword) {
        return tokenFactory.decryptString(encryptedPassword);
    }
}
