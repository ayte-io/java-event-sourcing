package io.ayte.es.api.v1.storage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PurgeableSnapshotStorage extends SnapshotStorage {
    CompletableFuture<Optional<Long>> purge(String entityType, String entityId);
}
