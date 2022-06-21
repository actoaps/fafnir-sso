package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
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
    List<ProviderConfiguration> providerConfigurations;
    Instant created;

    public final static OrganisationData DEFAULT = OrganisationData.builder()
            .created(Instant.now())
            .organisationId("default")
            .organisationName("Default Organisation")
            .build();

    public OrganisationData partialUpdate(OrganisationData updated) {
        return OrganisationData.builder()
                .organisationId(organisationId)
                .organisationName(Optional.ofNullable(updated.getOrganisationName()).orElse(organisationName))
                .contactEmail(Optional.ofNullable(updated.contactEmail).orElse(contactEmail))
                .providerConfigurations(Optional.ofNullable(updated.getProviderConfigurations()).orElse(providerConfigurations))
                .created(Optional.ofNullable(created).or(() -> Optional.ofNullable(updated.getCreated())).orElse(Instant.now()))
                .build();
    }

}
