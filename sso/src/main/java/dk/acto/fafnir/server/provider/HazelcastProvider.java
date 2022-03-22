package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.AuthenticationService;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.UsernamePasswordCredentials;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
@Lazy
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePasswordCredentials> {
    private final TokenFactory tokenFactory;
    private final AuthenticationService authenticationService;
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
        return Try.of(() -> authenticationService.authenticate(organisation, username, password))
                .toJavaOptional()
                .map(tokenFactory::generateToken)
                .map(CallbackResult::success)
                .orElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return true;
    }
}
