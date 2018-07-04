package io.ayte.es.api.v1.storage;

import java.util.concurrent.CompletableFuture;

/**
 * This interface allows to remove specific snapshot.
 */
public interface CleanableSnapshotStorage {
    CompletableFuture<Boolean> remove(String entityType, String entityId, long snapshotNumber);
}
