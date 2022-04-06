package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Value
@AllArgsConstructor
@Builder
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
}
