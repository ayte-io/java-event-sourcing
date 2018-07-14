package io.ayte.es.api.v1.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EventStorage {
    CompletableFuture<Optional<SerializedEvent>> get(String entityType, String entityId, long sequenceNumber);
    CompletableFuture<List<SerializedEvent>> list(String entityType, String entityId, long skip, long size);

    /**
     * Saves specified event, entity type, id and sequence number should be taken from payload.
     *
     * @return True if save succeeded, false otherwise
     */
    CompletableFuture<Boolean> save(SerializedEvent event);

    /**
     * @return Sequence number of last event for specified entity, empty
     * optional if nothing has been stored yet.
     */
    CompletableFuture<Optional<Long>> getSequenceNumber(String entityType, String entityId);
}
