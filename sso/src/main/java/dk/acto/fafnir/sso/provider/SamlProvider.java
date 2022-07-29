package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.exception.MissingRequiredSamlAttribute;
import dk.acto.fafnir.api.exception.OrganisationNotUsingSaml;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.saml.UpdateableRelyingPartyRegistrationRepository;
import dk.acto.fafnir.sso.util.TokenFactory;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
@ConditionalOnBean(UpdateableRelyingPartyRegistrationRepository.class)
public class SamlProvider implements RedirectingAuthenticationProvider<Saml2AuthenticatedPrincipal> {
    private final UpdateableRelyingPartyRegistrationRepository relyingPartyRegistrations;
    private final TokenFactory tokenFactory;
    private final AdministrationService administrationService;

    @Override
    public String authenticate() {
        return "saml/login";
    }

    @Override
    public AuthenticationResult callback(Saml2AuthenticatedPrincipal data) {
        var subject = Optional.ofNullable(data.getFirstAttribute("email"))
                .or(() -> Optional.ofNullable(data.getFirstAttribute("subject")))
                .map(String::valueOf)
                .orElseThrow(MissingRequiredSamlAttribute::new);
        var name = Optional.ofNullable(data.getFirstAttribute("name"))
                .map(String::valueOf)
                .orElse(subject);

        var userData = UserData.builder()
                .subject(subject)
                .name(name)
                .build();
        var orgActual = administrationService.readOrganisation(test ->
                test.getProviderId().equals(getMetaData().getProviderId()) &&
                        test.getValues().get("Registration Id").equals(data.getRelyingPartyRegistrationId()));
        var claimsActual = ClaimData.builder()
                .claims(data.getAttributes().entrySet().stream()
                        .filter(x -> !x.getKey().equals("email"))
                        .filter(x -> !x.getKey().equals("subject"))
                        .filter(x -> !x.getKey().equals("name"))
                        .map(x -> x.getKey() + "=" + x.getValue().stream().findFirst())
                        .toArray(String[]::new))
                .build();

        var jwt = tokenFactory.generateToken(userData, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    public String getSamlRegistrationIds(String orgId) {
        var org = administrationService.readOrganisation(orgId);

        return Optional.of(org.getProviderConfiguration())
                .map(ProviderConfiguration::getValues)
                .map(x -> relyingPartyRegistrations.findById(x.get("Registration Id"))
                        .orElse(relyingPartyRegistrations.addRelyingPartyRegistration(x.get("Registration Id"), x.get("Metadata Location")))
                )
                .map(RelyingPartyRegistration::getRegistrationId)
                .orElseThrow(OrganisationNotUsingSaml::new);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.SAML;
    }
}
