package dk.acto.fafnir.providers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.FailureReason;
import dk.acto.fafnir.TokenFactory;
import dk.acto.fafnir.model.CallbackResult;
import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.model.conf.HazelcastConf;
import dk.acto.fafnir.providers.credentials.UsernamePasswordCredentials;
import dk.acto.fafnir.util.CryptoUtil;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean(HazelcastConf.class)
@AllArgsConstructor
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePasswordCredentials> {
    private final TokenFactory tokenFactory;
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;

    @Override
    public String authenticate() {
            return "/hazelcast/login";
    }

    public CallbackResult callback(final UsernamePasswordCredentials data) {
        var username = data.getUsername();
        var password = data.getPassword();

        IMap<String, FafnirUser> map = hazelcastInstance.getMap(hazelcastConf.getMapName());
        var identifier = hazelcastConf.isUsernameIsEmail() ? username.toLowerCase() : username;
        return Optional.ofNullable(map.get(identifier))
                .filter(user -> validatePassword(user, password))
                .map(x -> tokenFactory.generateToken(
                        FafnirUser.builder()
                                .name(x.getName())
                                .subject(x.getSubject())
                                .provider("hazelcast")
                                .locale(x.getLocale())
                                .metaId(x.getMetaId())
                                .organisationId(x.getOrganisationId())
                                .organisationName(x.getOrganisationName())
                                .roles(x.getRoles())
                                .build()))
                .map(CallbackResult::success)
                .orElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    private boolean validatePassword(FafnirUser user, String password) {
        return hazelcastConf.isPasswordIsEncrypted() ?
                CryptoUtil.decryptPassword(user.getPassword(), tokenFactory.getPrivateKey())
                .equals(password) :
                CryptoUtil.hashPassword(password).equals(user.getPassword());
    }
}
