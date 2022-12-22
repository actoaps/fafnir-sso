package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.AuthenticationResult;
import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.ProviderMetaData;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.AuthenticationService;
import dk.acto.fafnir.sso.dto.HazelcastLoginInfo;
import dk.acto.fafnir.sso.dto.LoginResponseInfo;
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
    private final FafnirConf fafnirConf;

    @Override
    public String authenticate() {
        return fafnirConf.buildUrl("/hazelcast/login");
    }

    @Override
    public AuthenticationResult callback(final UsernamePasswordCredentials data) {
        return authenticate(data)
                .map(AuthenticationResult::success)
                .orElse(AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    public LoginResponseInfo callback(final HazelcastLoginInfo info) {
        var data = UsernamePasswordCredentials.builder()
                .password(info.getPassword())
                .username(info.getEmail())
                .organisation(info.getOrgId())
                .build();

        return authenticate(data)
                .map(x -> LoginResponseInfo.builder().jwt(x).build())
                .orElse(LoginResponseInfo.builder().error(FailureReason.AUTHENTICATION_FAILED).build());
    }

    public Optional<String> authenticate(final UsernamePasswordCredentials data) {
        var username = hazelcastConf.isTrimUsername()
                ? data.getUsername().stripTrailing()
                : data.getUsername();
        var password = data.getPassword();
        var organisation = data.getOrganisation();

        return Try.of(() -> {
            var subject = administrationService.readUser(username);
            var org = administrationService.readOrganisation(organisation);
            var claimData = authenticationService.authenticate(organisation, username, password);

            return tokenFactory.generateToken(subject, org, claimData, getMetaData());
        }).toJavaOptional();
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
