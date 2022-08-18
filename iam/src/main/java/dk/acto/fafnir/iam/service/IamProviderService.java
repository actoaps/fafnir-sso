package dk.acto.fafnir.iam.service;

import dk.acto.fafnir.api.exception.NoSuchProvider;
import dk.acto.fafnir.api.model.ProviderMetaData;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.ProviderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@AllArgsConstructor
@Service
public class IamProviderService implements ProviderService {
    @Override
    public String[] getAcceptedProviders() {
        return Arrays.stream(MetadataProvider.getAllSupportedProviders())
                .map(ProviderMetaData::getProviderId)
                .toArray(String[]::new);
    }

    @Override
    public ProviderMetaData getProviderMetaData(String providerId) {
            return Arrays.stream(MetadataProvider.getAllSupportedProviders())
                    .filter(metaData -> metaData.getProviderId().equals(providerId))
                    .findAny()
                    .orElseThrow(NoSuchProvider::new);
    }

    @Override
    public String getAuthenticationUrlForProvider(String providerId) {
        throw new NoSuchProvider();
    }
}
