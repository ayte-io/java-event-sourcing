package io.ayte.es.api.v1.storage;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
public class SerializedSnapshot {
    private final String entityType;
    private final String entityId;
    private final long sequenceNumber;
    private final long entityVersion;
    private final byte[] payload;
    private final ZonedDateTime eventAcknowledgedAt;
    private final ZonedDateTime eventOccurredAt;
    private final ZonedDateTime entityCreatedAt;
    private final ZonedDateTime createdAt;
    private final Map<String, Object> metadata;
}
