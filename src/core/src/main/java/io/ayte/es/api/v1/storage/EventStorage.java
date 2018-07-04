package io.ayte.es.api.v1.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

interface EventStorage {
    CompletableFuture<Optional<SerializedEvent>> get(String entityType, String entityId);
    CompletableFuture<List<SerializedEvent>> list(String entityType, String entityId, long skip, long size);

    /**
     * Saves specified event, entity type, id and sequence number should be taken from payload.
     *
     * @return True if save succeeded, false otherwise
     */
    CompletableFuture<Boolean> save(SerializedEvent event);

    /**
     * @return Sequence number of last event for specified entity, zero if nothing has been stored yet.
     */
    CompletableFuture<Long> getSequenceNumber(String entityType, String entityId);

    default CompletableFuture<List<SerializedEvent>> list(String entityType, String entityId, long skip) {
        return list(entityType, entityId, skip, Long.MAX_VALUE);
    }

    default CompletableFuture<List<SerializedEvent>> list(String entityType, String entityId) {
        return list(entityType, entityId, 0);
    }
}
