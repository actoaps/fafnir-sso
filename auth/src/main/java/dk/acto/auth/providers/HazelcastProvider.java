package dk.acto.auth.providers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.services.ServiceHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HazelcastProvider implements Provider {
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

    public String callback(final String username, final String password) {
        IMap<String, String> map = hazelcastInstance.getMap("fafnir-user");
        var identifier = actoConf.isHazelcastUsernameIsEmail() ? username.toLowerCase() : username;
        return Optional.ofNullable(map.get(identifier))
                .map(this::decryptPassword)
                .filter(password::equals)
                .map(x -> tokenFactory.generateToken(
                        identifier,
                        "hazelcast",
                        identifier))
                .map(x -> ServiceHelper.getJwtUrl(actoConf, x))
                .orElse(actoConf.getFailureUrl());
    }

    private String decryptPassword(String encryptedPassword) {
        return tokenFactory.decryptString(encryptedPassword);
    }
}
