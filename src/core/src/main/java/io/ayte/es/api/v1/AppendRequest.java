package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@SuppressWarnings("squid:S00119")
@Builder(toBuilder = true)
public class AppendRequest<E, ID> {
    private final Class<E> entity;
    private final ID id;
    private final Mutation<E, ID> mutation;
    private final ZonedDateTime occurredAt;
    private final Map<String, Object> metadata;
}
