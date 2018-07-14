package io.ayte.es.api.v1.storage;

import java.util.concurrent.CompletableFuture;

public interface ReplaceableEventStorage extends EventStorage {
    CompletableFuture<Boolean> replace(SerializedEvent current, SerializedEvent replacement);
}
