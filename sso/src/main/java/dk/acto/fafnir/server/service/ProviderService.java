package dk.acto.fafnir.server.service;

import dk.acto.fafnir.api.exception.NoSuchProvider;
import dk.acto.fafnir.server.provider.ProviderInformation;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Value
@AllArgsConstructor
@Service
public class ProviderService {
    Set<ProviderInformation> providerInformationSet;

    public String[] getAcceptedProviders() {
        return providerInformationSet.stream()
                .map(ProviderInformation::entryPoint)
                .toArray(String[]::new);
    }

    public boolean providerSupportsOrganisations(String providerId) {
        return providerInformationSet.stream()
                .filter(x -> x.entryPoint().equals(providerId))
                .map(ProviderInformation::supportsOrganisationUrls)
                .findAny()
                .orElseThrow(NoSuchProvider::new);
    }
}
