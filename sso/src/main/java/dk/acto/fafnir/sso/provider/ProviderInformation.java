package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.OrganisationSupport;
import dk.acto.fafnir.api.model.ProviderMetaData;

public interface ProviderInformation {
    default boolean supportsOrganisationUrls() {
        return OrganisationSupport.FAFNIR.equals(getMetaData().getOrganisationSupport());
    }
    String providerId();
    ProviderMetaData getMetaData();
}
