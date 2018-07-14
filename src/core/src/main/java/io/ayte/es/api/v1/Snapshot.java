package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true, builderClassName = "LombokBuilder")
@EqualsAndHashCode(of = {"entityType", "entityId", "sequenceNumber", "entityVersion"})
@ToString(of = {"entityType", "entityId", "sequenceNumber", "entityVersion"})
@SuppressWarnings("squid:S00119")
public class Snapshot<E, ID> {
    private final EntityType<E> entityType;
    private final Identifier<ID> entityId;
    private final long sequenceNumber;
    private final long entityVersion;
    private final E entity;
    private final ZonedDateTime eventAcknowledgedAt;
    private final ZonedDateTime eventOccurredAt;
    private final ZonedDateTime entityCreatedAt;
    private final ZonedDateTime createdAt;
    private final Map<String, Object> metadata;
}
