package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder(toBuilder = true)
@SuppressWarnings("squid:S00119")
public class Entity<E, ID> {
    private final Identifier<ID> id;
    private final EntityType<E> type;
    private final E value;
    private final long eventSequenceNumber;
    private final long snapshotSequenceNumber;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    private final ZonedDateTime snapshotCreatedAt;
}
