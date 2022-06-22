package dk.acto.fafnir.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class ClaimInfo {
    String id;
    String name;
    String csvClaims;
}
