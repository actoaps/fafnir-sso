package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

@Value
@AllArgsConstructor
@Builder
public class ClaimData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String subject;
    String organisationId;
    @EqualsAndHashCode.Exclude
    String[] claims;

    public static ClaimData empty(String subject, String orgId) {
        return ClaimData.builder()
                .subject(subject)
                .organisationId(orgId)
                .claims(new String[] {})
                .build();
    }
}
