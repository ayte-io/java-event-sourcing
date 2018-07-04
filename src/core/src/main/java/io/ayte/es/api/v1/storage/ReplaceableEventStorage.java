package io.ayte.es.api.v1.storage;

import java.util.concurrent.CompletableFuture;

public interface ReplaceableEventStorage {
    CompletableFuture<Void> replace(SerializedEvent event);
}
