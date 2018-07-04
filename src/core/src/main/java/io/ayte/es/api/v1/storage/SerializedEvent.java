package io.ayte.es.api.v1.storage;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
public class SerializedEvent {
    private final String entityType;
    private final String entityId;
    private final long sequenceNumber;
    private final String mutationType;
    private final int mutationVersion;
    private final byte[] payload;
    private final ZonedDateTime occurredAt;
    private final ZonedDateTime acknowledgedAt;
    private final Map<String, Object> metadata;
}
