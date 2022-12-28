package dk.acto.fafnir.api.model;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

@Value
@Builder(toBuilder = true)
public class OrganisationSubjectPair implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String organisationId;
    String subject;
}
