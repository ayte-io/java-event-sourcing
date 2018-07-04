package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
@SuppressWarnings("squid:S00119")
public class Snapshot<E, ID> {
    private final Identifier<ID> entityId;
    private final EntityType<E> entityType;
    private final long sequenceNumber;
    private final long entityVersion;
    private final E entity;
    private final ZonedDateTime eventAcknowledgedAt;
    private final ZonedDateTime eventOccurredAt;
    private final ZonedDateTime entityCreatedAt;
    private final ZonedDateTime createdAt;
    private final Map<String, Object> metadata;
}
