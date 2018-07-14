package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class EntityType<E> {
    private final String type;
    private final Class<E> symbol;
}
