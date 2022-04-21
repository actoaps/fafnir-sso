package dk.acto.fafnir.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class ClaimOrganisationInfo {
    String organisationId;
    String organisationName;
    List<ClaimUserInfo> users;
}
