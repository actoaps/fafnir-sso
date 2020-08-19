package dk.acto.fafnir.providers.economic;

import lombok.Data;

import java.util.List;

@Data
public class CollectionWrapper<T> {
    private List<T> collection;
}
