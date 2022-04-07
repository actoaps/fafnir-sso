package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor
@Builder
public class ProviderMetaData {
    String providerId;
    String providerName;
    OrganisationSupport organisationSupport;
    List<String> inputs;
}
