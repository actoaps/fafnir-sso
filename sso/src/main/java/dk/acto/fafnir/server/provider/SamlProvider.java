package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.server.saml.UpdateableRelyingPartyRegistrationRepository;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.SamlCredentials;
import dk.acto.fafnir.server.util.TokenFactory;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@ConditionalOnBean(UpdateableRelyingPartyRegistrationRepository.class)
public class SamlProvider implements RedirectingAuthenticationProvider<SamlCredentials> {
    private final UpdateableRelyingPartyRegistrationRepository relyingPartyRegistrations;
    private final TokenFactory tokenFactory;
    private final AdministrationService administrationService;

    @Override
    public String authenticate() {
        return "/{orgId}/saml/login";
    }

    @Override
    public CallbackResult callback(SamlCredentials data) {
        var userData = UserData.builder()
                .subject(data.getEmail())
                .name(data.getName())
                .build();
        var orgActual = administrationService.readOrganisation(getMetaData());
        var claimsActual = ClaimData.empty(userData.getSubject(), orgActual.getOrganisationId());

        var jwt = tokenFactory.generateToken(userData, orgActual, claimsActual, getMetaData());

        return CallbackResult.success(jwt);
    }

    public List<String> getSamlRegistrationIds(String orgId) {
        var org = administrationService.readOrganisation(orgId);

        return org.getProviderConfigurations().stream()
                .filter(x -> x.getProviderId().equals(providerId()))
                .map(ProviderConfiguration::getValues)
                .map(x -> relyingPartyRegistrations.findById(x.get("Registration Id"))
                        .orElse(relyingPartyRegistrations.addRelyingPartyRegistration(x.get("Registration Id"), x.get("Metadata Location")))
                )
                .map(RelyingPartyRegistration::getRegistrationId)
                .toList();
    }

    @Override
    public String providerId() {
        return "saml";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerId(providerId())
                .providerName("SAML Provider")
                .inputs(List.of("Metadata Location", "Registration Id"))
                .organisationSupport(OrganisationSupport.FAFNIR)
                .build();
    }
}
