package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class ClaimsPayload {
    private final static ClaimsPayload ADMIN = ClaimsPayload.builder()
            .user(UserData.builder()
                    .subject("admin")
                    .password("admin")
                    .provider("fafnir-internal")
                    .build())
            .organisation(OrganisationData.builder()
                    .organisationId("default")
                    .build())
            .claim("ADMIN")
            .build();

    UserData user;
    OrganisationData organisation;
    String claim;
}
