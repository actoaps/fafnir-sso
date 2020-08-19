package dk.acto.auth.providers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.auth.FailureReason;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.CallbackResult;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.model.conf.HazelcastConf;
import dk.acto.auth.providers.credentials.UsernamePassword;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean(HazelcastConf.class)
@AllArgsConstructor
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePassword> {
    private final TokenFactory tokenFactory;
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;

    @Override
    public String authenticate() {
            return "/hazelcast/login";
    }

    public CallbackResult callback(final UsernamePassword data) {
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
                .map(CallbackResult::success)
                .orElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    private String decryptPassword(String encryptedPassword) {
        return tokenFactory.decryptString(encryptedPassword);
    }
}
