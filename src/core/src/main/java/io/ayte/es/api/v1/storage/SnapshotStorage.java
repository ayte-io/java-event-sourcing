package io.ayte.es.api.v1.storage;

import io.ayte.es.utility.CompletableFutures;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SnapshotStorage {
    CompletableFuture<Optional<SerializedSnapshot>> get(String entityType, String entityId, long sequenceNumber);
    CompletableFuture<List<SerializedSnapshot>> list(String entityType, String entityId, long skip, long size);

    /**
     * @return True if snapshot was successfully saved under unspecified number, false if it was occupied, exception on
     * error
     */
    CompletableFuture<Boolean> save(SerializedSnapshot snapshot);

    /**
     * @return Sequence number for last stored snapshot or zero if no snapshots for entity were stored
     */
    CompletableFuture<Optional<Long>> getSequenceNumber(String entityType, String entityId);

    @SuppressWarnings({"CodeBlock2Expr", "squid:S1602"})
    default CompletableFuture<Optional<SerializedSnapshot>> getLast(String entityType, String entityId) {
        return getSequenceNumber(entityType, entityId)
                .thenCompose(container -> {
                    return container
                            .map(number -> get(entityType, entityId, number))
                            .orElseGet(CompletableFutures::emptyOptional);
                });
    }
}
