package dk.acto.fafnir.api.model;

import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrganisationData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String organisationId;
    String organisationName;
    String contactEmail;
    ProviderConfiguration providerConfiguration;
    Instant created;

    public static final OrganisationData DEFAULT = OrganisationData.builder()
            .created(Instant.now())
            .organisationId("default")
            .organisationName("Default Organisation")
            .providerConfiguration(ProviderConfiguration.builder()
                    .providerId(MetadataProvider.HAZELCAST.getProviderId())
                    .build())
            .build();

    public OrganisationData partialUpdate(OrganisationData updated) {
        return OrganisationData.builder()
                .organisationId(organisationId)
                .organisationName(Optional.ofNullable(updated.getOrganisationName()).orElse(organisationName))
                .contactEmail(Optional.ofNullable(updated.contactEmail).orElse(contactEmail))
                .providerConfiguration(Optional.ofNullable(updated.getProviderConfiguration()).orElse(providerConfiguration))
                .created(Optional.ofNullable(created).or(() -> Optional.ofNullable(updated.getCreated())).orElse(Instant.now()))
                .build();
    }
}
