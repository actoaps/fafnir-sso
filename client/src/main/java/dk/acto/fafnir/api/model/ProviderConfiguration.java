package dk.acto.fafnir.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@Value
@AllArgsConstructor
@Builder
public class ProviderConfiguration implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String providerId;
    Map<String, String> values;

    public boolean hasKey(String key) {
        return values.containsKey(key);
    }

    public boolean hasValue(String key, String value) {
        return Optional.ofNullable(values.get(key))
                .map(x -> x.equals(value))
                .orElse(false);
    }
}
