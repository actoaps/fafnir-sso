package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class OrganisationData implements Serializable {
    public final static OrganisationData DEFAULT = OrganisationData.builder()
            .created(Instant.now())
            .organisationId("default")
            .organisationName("Default Organisation")
            .build();

    @Serial
    private static final long serialVersionUID = 1L;

    String organisationId;
    String organisationName;
    String contactEmail;
    String provider;
    Instant created;

}
