package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.api.model.OrganisationSupport;
import dk.acto.fafnir.api.model.ProviderMetaData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AuthenticationService;
import dk.acto.fafnir.server.model.FailureReason;
import dk.acto.fafnir.server.service.HazelcastAdministrationService;
import dk.acto.fafnir.server.util.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.UsernamePasswordCredentials;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@AllArgsConstructor
@Lazy
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePasswordCredentials> {
    private final TokenFactory tokenFactory;
    private final AuthenticationService authenticationService;
    private final HazelcastConf hazelcastConf;
    private final HazelcastAdministrationService administrationService;

    @Override
    public String authenticate() {
        return "/{orgId}/hazelcast/login";
    }

    public CallbackResult callback(final UsernamePasswordCredentials data) {
        var username = hazelcastConf.isTrimUsername()
                ? data.getUsername().stripTrailing()
                : data.getUsername();
        var password = data.getPassword();
        var organisation = data.getOrganisation();

        var subject = administrationService.readUser(username);
        var org = administrationService.readOrganisation(organisation);
        return Try.of(() -> authenticationService.authenticate(organisation, username, password))
                .toJavaOptional()
                .map(claimData -> tokenFactory.generateToken(
                        subject,
                        org,
                        claimData,
                        getMetaData()
                ))
                .map(CallbackResult::success)
                .orElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return true;
    }

    @Override
    public String providerId() {
        return "hazelcast";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerName("Hazelcast (Built-In)")
                .providerId(providerId())
                .organisationSupport(OrganisationSupport.FAFNIR)
                .inputs(List.of())
                .build();
    }
}
