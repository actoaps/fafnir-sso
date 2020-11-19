package dk.acto.fafnir.server.providers.economic;

import lombok.Data;

import java.util.List;

@Data
public class CollectionWrapper<T> {
    private List<T> collection;
}
