package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class OrganisationData {
    String organisationId;
    String organisationName;
    String contactEmail;
    String thirdPartyId;
    Instant created;
}
