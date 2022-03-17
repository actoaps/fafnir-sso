package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class OrganisationProvider {
    String thirdPartyId;
    String thirdPartyProvider;
}
