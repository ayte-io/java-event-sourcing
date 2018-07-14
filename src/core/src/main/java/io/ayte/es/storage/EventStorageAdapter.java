package io.ayte.es.storage;

import io.ayte.es.api.v1.storage.EventStorage;
import io.ayte.es.api.v1.storage.PurgeableEventStorage;
import io.ayte.es.api.v1.storage.ReplaceableEventStorage;
import io.ayte.es.api.v1.storage.SerializedEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EventStorageAdapter implements ReplaceableEventStorage, PurgeableEventStorage {
    private final EventStorage storage;
    private final ReplaceableEventStorage replaceable;
    private final PurgeableEventStorage purgeable;

    public EventStorageAdapter(EventStorage storage) {
        this.storage = storage;
        this.replaceable = storage instanceof ReplaceableEventStorage ? (ReplaceableEventStorage) storage : null;
        this.purgeable = storage instanceof PurgeableEventStorage ? (PurgeableEventStorage) storage : null;
    }

    @Override
    public CompletableFuture<Optional<SerializedEvent>> get(String entityType, String entityId, long sequenceNumber) {
        return storage.get(entityType, entityId, sequenceNumber);
    }

    @Override
    public CompletableFuture<List<SerializedEvent>> list(String entityType, String entityId, long skip, long size) {
        return storage.list(entityType, entityId, skip, size);
    }

    @Override
    public CompletableFuture<Boolean> save(SerializedEvent event) {
        return storage.save(event);
    }

    @Override
    public CompletableFuture<Optional<Long>> getSequenceNumber(String entityType, String entityId) {
        return storage.getSequenceNumber(entityType, entityId);
    }

    @Override
    public CompletableFuture<Boolean> replace(SerializedEvent current, SerializedEvent replacement) {
        this.assertSupportsReplace();
        return replaceable.replace(current, replacement);
    }

    @Override
    public CompletableFuture<Optional<Long>> purge(String entityType, String entityId) {
        this.assertSupportsPurge();
        return purgeable.purge(entityType, entityId);
    }

    public boolean supportsPurge() {
        return purgeable != null;
    }

    public boolean supportsReplace() {
        return replaceable != null;
    }

    private void assertSupportsPurge() {
        if (purgeable == null) {
            throw new UnsupportedOperationException("Storage " + storage + " doesn't support purge operation");
        }
    }

    private void assertSupportsReplace() {
        if (replaceable == null) {
            throw new UnsupportedOperationException("Storage " + storage + " doesn't support replace operation");
        }
    }
}
