package dk.acto.fafnir.sso.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.exception.NoSuchUser;
import dk.acto.fafnir.api.exception.PasswordMismatch;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationSubjectPair;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AuthenticationService;
import dk.acto.fafnir.api.service.CryptoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService.CLAIM_POSTFIX;
import static dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService.USER_POSTFIX;

@AllArgsConstructor
@Service
public class HazelcastAuthenticationService implements AuthenticationService {
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;
    private final RsaKeyManager rsaKeyManager;
    private final CryptoService cryptoService;

    @Override
    public ClaimData authenticate(final String orgId, final String username, final String password) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);

        var pk = hazelcastConf.isPasswordIsEncrypted() ? rsaKeyManager.getPrivateKey() : null;

        var user = Optional.ofNullable(userMap.get(username))
                .orElseThrow(NoSuchUser::new);

        if (!cryptoService.matches(password, user.getPassword(), pk)) {
            throw new PasswordMismatch();
        }

        return claimMap.getOrDefault(OrganisationSubjectPair.builder()
                        .organisationId(orgId)
                        .subject(user.getSubject())
                .build(), ClaimData.empty());
    }

}
