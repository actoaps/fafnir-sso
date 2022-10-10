package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.AuthenticationResult;
import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.ProviderMetaData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.AuthenticationService;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@AllArgsConstructor
@Lazy
public class HazelcastProvider implements RedirectingAuthenticationProvider<UsernamePasswordCredentials> {
    private final TokenFactory tokenFactory;
    private final AuthenticationService authenticationService;
    private final HazelcastConf hazelcastConf;
    private final AdministrationService administrationService;

    @Override
    public String authenticate() {
        return "/hazelcast/login";
    }

    @Override
    public AuthenticationResult callback(final UsernamePasswordCredentials data) {
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
                .map(AuthenticationResult::success)
                .orElse(AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    public List<OrganisationData> getOrganisations(String subject) {
        return Arrays.stream(administrationService.getOrganisationsForUser(subject))
                .filter(x -> x.getProviderConfiguration().getProviderId()
                        .equals(MetadataProvider.HAZELCAST.getProviderId()))
                .collect(Collectors.toList());
    }

    public Optional<OrganisationData> getOrganisation(String orgId) {
        return Try.of(() -> administrationService.readOrganisation(orgId))
                .toJavaOptional();
    }

    public List<OrganisationData> getOrganisationsForProvider() {
        return Arrays.stream(administrationService.readOrganisations())
                .filter(x -> x.getProviderConfiguration().getProviderId()
                        .equals(MetadataProvider.HAZELCAST.getProviderId()))
                .collect(Collectors.toList());
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.HAZELCAST;
    }
}
