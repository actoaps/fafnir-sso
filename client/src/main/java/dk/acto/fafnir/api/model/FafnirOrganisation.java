package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Delegate;

import java.io.Serial;
import java.io.Serializable;

@Value
@AllArgsConstructor
@Builder
public class FafnirOrganisation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Delegate
    OrganisationData data;
    String[] subjects;
}
