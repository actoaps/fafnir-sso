package dk.acto.fafnir.server.service;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.exception.NoSuchOrganisation;
import dk.acto.fafnir.api.exception.NoSuchUser;
import dk.acto.fafnir.api.exception.PasswordMismatch;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

import static dk.acto.fafnir.server.service.HazelcastAdministrationService.ORG_POSTFIX;
import static dk.acto.fafnir.server.service.HazelcastAdministrationService.USER_POSTFIX;

@Value
@AllArgsConstructor
@Service
public class HazelcastAuthenticationService implements AuthenticationService {
    HazelcastInstance hazelcastInstance;
    HazelcastConf hazelcastConf;
    RsaKeyManager rsaKeyManager;

    @Override
    public ClaimData authenticate(final String orgId, final String username, final String password) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");

        var pk = hazelcastConf.isPasswordIsEncrypted() ? rsaKeyManager.getPrivateKey() : null;

        var user = Optional.ofNullable(userMap.get(username))
                .orElseThrow(NoSuchUser::new);
        if (!user.canAuthenticate(password, pk)){
            throw new PasswordMismatch();
        }

        return claimSet.stream().filter(x -> x.getOrganisationId().equals(orgId))
                        .filter(x -> x.getSubject().equals(username))
                .findAny()
                .orElse(ClaimData.empty(user.getSubject(), orgId));
    }
}
