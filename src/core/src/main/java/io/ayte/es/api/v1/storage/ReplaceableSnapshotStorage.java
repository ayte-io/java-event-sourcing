package io.ayte.es.api.v1.storage;

import java.util.concurrent.CompletableFuture;

public interface ReplaceableSnapshotStorage extends SnapshotStorage {
    CompletableFuture<Boolean> replace(SerializedSnapshot current, SerializedSnapshot replacement);
}
