package dk.acto.fafnir.api.model;

import io.vavr.collection.Array;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;

@Value
@AllArgsConstructor
@Builder
public class ClaimData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String subject;
    String organisationId;
    String[] claims;

    public static ClaimData empty(String subject, String orgId) {
        return ClaimData.builder()
                .subject(subject)
                .organisationId(orgId)
                .claims(new String[] {})
                .build();
    }
}
