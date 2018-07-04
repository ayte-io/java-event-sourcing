package io.ayte.es.api.v1.storage;

import java.util.concurrent.CompletableFuture;

public interface PurgeableEventStorage {
    CompletableFuture<Long> purge(String entityType, String entityId);
}
