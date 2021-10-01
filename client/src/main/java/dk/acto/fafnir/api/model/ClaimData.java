package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class ClaimData {
    String subject;
    String organisationId;
    String[] claims;
}
