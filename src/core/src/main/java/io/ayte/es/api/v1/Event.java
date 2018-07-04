package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
@SuppressWarnings("squid:S00119")
public class Event<M extends Mutation<E, ID>, E, ID> {
    private final Identifier<ID> entityId;
    private final EntityType<E> entityType;
    private final MutationType<M, E, ID> mutationType;
    private final M mutation;
    private final long eventNumber;
    private final ZonedDateTime occurredAt;
    private final ZonedDateTime acknowledgedAt;
    private final Map<String, Object> metadata;
}
