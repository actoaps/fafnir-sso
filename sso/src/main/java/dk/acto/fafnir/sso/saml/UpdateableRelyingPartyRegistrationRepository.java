package dk.acto.fafnir.sso.saml;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UpdateableRelyingPartyRegistrationRepository
        implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {
    private final Map<String, RelyingPartyRegistration> relyingPartyProperties = new ConcurrentHashMap<>();

    public RelyingPartyRegistration addRelyingPartyRegistration(String registrationId, String metadataLocation) {
        var relyingParty = RelyingPartyRegistrations.fromMetadataLocation(metadataLocation)
                .registrationId(registrationId)
                .build();

        relyingPartyProperties.put(registrationId, relyingParty);

        return relyingParty;
    }

    public Optional<RelyingPartyRegistration> findById(String registrationId) {
        return Optional.ofNullable(relyingPartyProperties.get(registrationId));
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        return relyingPartyProperties.get(registrationId);
    }

    @Override
    public Iterator<RelyingPartyRegistration> iterator() {
        return relyingPartyProperties.values().iterator();
    }
}
