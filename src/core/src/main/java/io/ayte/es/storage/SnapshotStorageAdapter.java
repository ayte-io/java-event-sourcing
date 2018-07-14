package io.ayte.es.storage;

import io.ayte.es.api.v1.storage.CleanableSnapshotStorage;
import io.ayte.es.api.v1.storage.PurgeableSnapshotStorage;
import io.ayte.es.api.v1.storage.ReplaceableSnapshotStorage;
import io.ayte.es.api.v1.storage.SerializedSnapshot;
import io.ayte.es.api.v1.storage.SnapshotStorage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SnapshotStorageAdapter implements
        CleanableSnapshotStorage,
        PurgeableSnapshotStorage,
        ReplaceableSnapshotStorage {

    private final SnapshotStorage storage;
    private final CleanableSnapshotStorage cleanable;
    private final PurgeableSnapshotStorage purgeable;
    private final ReplaceableSnapshotStorage replaceable;
    private final String assertionPrefix;

    public SnapshotStorageAdapter(SnapshotStorage storage) {
        this.storage = storage;
        cleanable = storage instanceof CleanableSnapshotStorage ? (CleanableSnapshotStorage) storage : null;
        purgeable = storage instanceof PurgeableSnapshotStorage ? (PurgeableSnapshotStorage) storage : null;
        replaceable = storage instanceof ReplaceableSnapshotStorage ? (ReplaceableSnapshotStorage) storage : null;
        assertionPrefix = "Provided storage implementation (" + storage + ") doesn't support ";
    }

    public boolean supportsPurge() {
        return purgeable != null;
    }

    public boolean supportsRemoval() {
        return cleanable != null;
    }

    public boolean supportsReplacement() {
        return replaceable != null;
    }

    private void assertPurgeable() {
        if (!supportsPurge()) {
            throw new UnsupportedOperationException(assertionPrefix + "purge");
        }
    }

    private void assertCleanable() {
        if (!supportsRemoval()) {
            throw new UnsupportedOperationException(assertionPrefix + "cleanup");
        }
    }

    private void assertReplaceable() {
        if (!supportsRemoval()) {
            throw new UnsupportedOperationException(assertionPrefix + "replacement");
        }
    }

    @Override
    public CompletableFuture<Boolean> remove(String entityType, String entityId, long sequenceNumber) {
        assertCleanable();
        return cleanable.remove(entityType, entityId, sequenceNumber);
    }

    @Override
    public CompletableFuture<Optional<Long>> purge(String entityType, String entityId) {
        assertPurgeable();
        return purgeable.purge(entityType, entityId);
    }

    @Override
    public CompletableFuture<Boolean> replace(SerializedSnapshot current, SerializedSnapshot replacement) {
        assertReplaceable();
        return replaceable.replace(current, replacement);
    }

    @Override
    public CompletableFuture<Optional<SerializedSnapshot>> get(String entityType, String entityId, long sequenceNumber) {
        return storage.get(entityType, entityId, sequenceNumber);
    }

    @Override
    public CompletableFuture<List<SerializedSnapshot>> list(String entityType, String entityId, long skip, long size) {
        return storage.list(entityType, entityId, skip, size);
    }

    @Override
    public CompletableFuture<Boolean> save(SerializedSnapshot snapshot) {
        return storage.save(snapshot);
    }

    @Override
    public CompletableFuture<Optional<Long>> getSequenceNumber(String entityType, String entityId) {
        return storage.getSequenceNumber(entityType, entityId);
    }

    @Override
    public CompletableFuture<Optional<SerializedSnapshot>> getLast(String entityType, String entityId) {
        return storage.getLast(entityType, entityId);
    }
}
