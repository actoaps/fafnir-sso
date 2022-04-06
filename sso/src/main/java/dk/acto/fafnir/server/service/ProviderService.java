package dk.acto.fafnir.server.service;

import dk.acto.fafnir.api.exception.NoSuchProvider;
import dk.acto.fafnir.server.provider.ProviderInformation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@AllArgsConstructor
@Service
public class ProviderService {
    private final Set<ProviderInformation> providerInformationSet;

    public String[] getAcceptedProviders() {
        return providerInformationSet.stream()
                .map(ProviderInformation::providerId)
                .toArray(String[]::new);
    }

    public ProviderInformation getProviderInformation(String providerId) {
        return providerInformationSet.stream()
                .filter(x -> x.providerId().equals(providerId))
                .findAny()
                .orElseThrow(NoSuchProvider::new);
    }
}
