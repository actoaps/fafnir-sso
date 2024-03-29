package dk.acto.fafnir.sso.service;

import dk.acto.fafnir.api.exception.NoSuchProvider;
import dk.acto.fafnir.api.model.ProviderMetaData;
import dk.acto.fafnir.api.provider.ProviderInformation;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.service.ProviderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

@AllArgsConstructor
@Service
public class SsoProviderService implements ProviderService {
    private final Set<ProviderInformation> providerInformationSet;

    @Override
    public String[] getAcceptedProviders() {
        return providerInformationSet.stream()
            .map(ProviderInformation::getMetaData)
            .map(ProviderMetaData::getProviderId)
            .toArray(String[]::new);
    }

    @Override
    public ProviderMetaData getProviderMetaData(String providerId) {
        return providerInformationSet.stream()
            .map(ProviderInformation::getMetaData)
            .filter(metaData -> metaData.getProviderId().equals(providerId))
            .findAny()
            .orElseThrow(NoSuchProvider::new);
    }

    @Override
    public String getAuthenticationUrlForProvider(String providerId) {
        return providerInformationSet.stream()
            .filter(pi -> pi.getMetaData().getProviderId().equals(providerId))
            .filter(RedirectingAuthenticationProvider.class::isInstance)
            .map(RedirectingAuthenticationProvider.class::cast)
            .map(provider -> {
                try {
                    return provider.authenticate();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("Failed to generate authentication URL due to missing algorithm.", e);
                }
            })
            .findFirst()
            .orElseThrow(NoSuchProvider::new);
    }
}
