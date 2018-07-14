package io.ayte.es.api.v1.storage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PurgeableEventStorage extends EventStorage {
    CompletableFuture<Optional<Long>> purge(String entityType, String entityId);
}
