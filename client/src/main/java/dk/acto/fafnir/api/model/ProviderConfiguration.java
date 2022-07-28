package dk.acto.fafnir.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Value
@AllArgsConstructor
@Builder
public class ProviderConfiguration implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String providerId;
    Map<String, String> values;
}
