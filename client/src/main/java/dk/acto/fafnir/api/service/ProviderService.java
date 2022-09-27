package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.OrganisationSupport;
import dk.acto.fafnir.api.model.ProviderMetaData;

public interface ProviderService {
    default boolean supportsClaims(String providerId) {
        return getProviderMetaData(providerId).getOrganisationSupport().equals(OrganisationSupport.FAFNIR);
    }

    String[] getAcceptedProviders();

    ProviderMetaData getProviderMetaData(String providerId);

    String getAuthenticationUrlForProvider(String providerId);
}
