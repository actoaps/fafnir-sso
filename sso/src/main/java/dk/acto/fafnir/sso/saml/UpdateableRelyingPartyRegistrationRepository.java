package dk.acto.fafnir.sso.saml;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.crypto.X509CertificateManager;
import lombok.AllArgsConstructor;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
public class UpdateableRelyingPartyRegistrationRepository
        implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {

    private final RsaKeyManager rsaKeyManager;
    private final X509CertificateManager certificateManager;
    private final Map<String, RelyingPartyRegistration> relyingPartyProperties = new ConcurrentHashMap<>();

    public RelyingPartyRegistration addRelyingPartyRegistration(String registrationId, String metadataLocation) {
        var relyingParty = RelyingPartyRegistrations.fromMetadataLocation(metadataLocation)
                .registrationId(registrationId)
                .signingX509Credentials(x -> x.add(Saml2X509Credential.signing(rsaKeyManager.getPrivateKey(), certificateManager.getCertificate())))
                .decryptionX509Credentials(x -> x.add(Saml2X509Credential.decryption(rsaKeyManager.getPrivateKey(), certificateManager.getCertificate())))
                .build();

        relyingPartyProperties.put(registrationId, relyingParty);

        return relyingParty;
    }

    public Optional<RelyingPartyRegistration> findById(String registrationId) {
        return Optional.ofNullable(registrationId)
                .map(relyingPartyProperties::get);
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
