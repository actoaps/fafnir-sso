package dk.acto.fafnir.api.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProviderMetaData {
    String providerId;
    String providerName;
    OrganisationSupport organisationSupport;
    List<String> inputs;
}
