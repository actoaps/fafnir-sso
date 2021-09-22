package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.api.model.AuthorizationTable;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.UsernamePasswordCredentials;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePasswordCredentials> {
    private final TokenFactory tokenFactory;
    private final AuthorizationTable authorizationTable;
    private final HazelcastConf hazelcastConf;

    @Override
    public String authenticate() {
        return "/hazelcast/login";
    }

    public CallbackResult callback(final UsernamePasswordCredentials data) {
        var username = hazelcastConf.isTrimUsername()
                ? data.getUsername().stripTrailing()
                : data.getUsername();
        var password = data.getPassword();
        var organisation = data.getOrganisation();
        return Optional.ofNullable(authorizationTable.getUser(organisation, username, password))
                .map(tokenFactory::generateToken)
                .map(CallbackResult::success)
                .orElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }
}
