package io.ayte.es.storage;

import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.api.v1.storage.SerializedSnapshot;
import io.ayte.es.utility.CompletableFutures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"CodeBlock2Expr", "squid:S00119", "squid:S1602"})
public class SnapshotRepository {
    private final DescriptorRegistry registry;
    private final SnapshotStorageAdapter storage;
    private final SnapshotSerializer serializer;
    private final SnapshotRemovalFacility removalFacility;

    public <E, ID> CompletableFuture<Optional<Snapshot<E, ID>>> get(Class<E> entity, ID id, long sequenceNumber) {
        val descriptor = registry.getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        val future = storage
                .get(descriptor.getType().getType(), encodedId, sequenceNumber)
                .thenCompose(container -> {
                    return container
                            .map(snapshot -> CompletableFutures.execute(() -> serializer.<E, ID>deserialize(snapshot)))
                            .orElseGet(CompletableFutures::nullFuture);
                })
                .thenCompose(snapshot -> {
                    if (snapshot == null) {
                        return CompletableFutures.emptyOptional();
                    }
                    if (!removalFacility.eligible(snapshot)) {
                        return CompletableFutures.optional(snapshot);
                    }
                    return removalFacility.process(snapshot);
                });
        return CompletableFutures.onError(future, error -> {
            log.error(
                    "Error during snapshot #{} retrieval for entity `{}` with id `{}`: {}",
                    sequenceNumber,
                    entity,
                    id,
                    error
            );
        });
    }

    public <E, ID> CompletableFuture<Optional<Snapshot<E, ID>>> getLast(Class<E> entity, ID id) {
        val descriptor = registry.getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        val future = storage
                .getLast(descriptor.getType().getType(), encodedId)
                .thenCompose(container -> {
                    return container
                            .map(snapshot -> CompletableFutures.execute(() -> serializer.<E, ID>deserialize(snapshot)))
                            .orElseGet(CompletableFutures::nullFuture);
                })
                .thenApply(Optional::ofNullable);
        return CompletableFutures.onError(future, error -> {
            log.error("Error during last snapshot retrieval for entity `{}` with id `{}`: {}", entity, id, error);
        });
    }

    public <E, ID> CompletableFuture<List<Snapshot<E, ID>>> list(Class<E> entity, ID id, long skip, long size) {
        val descriptor = registry.<E, ID>getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        CompletableFuture<List<Snapshot<E, ID>>> future = storage
                .list(descriptor.getType().getType(), encodedId, skip, size)
                .thenCompose(snapshots -> CompletableFutures.execute(() -> serializer.deserializeAll(snapshots)));
        return CompletableFutures.onError(future, error -> {
            log.error(
                    "Error during fetching snapshots for entity `{}` with id `{}` (skip: {}, limit: {}): {}",
                    entity,
                    id,
                    skip,
                    size,
                    error
            );
        });
    }

    public <E, ID> CompletableFuture<Boolean> save(Snapshot<E, ID> snapshot) {
        log.trace("Saving {}", snapshot);
        val pipeline = CompletableFutures
                .execute(() -> serializer.serialize(snapshot))
                .thenCompose(storage::save)
                .thenCompose(success -> {
                    if (!success) {
                        return CompletableFutures.FALSE;
                    }
                    return postSaveRepair(snapshot).thenApply(anything -> true);
                });
        return CompletableFutures.onError(pipeline, error -> {
            log.error("Error during saving {}: {}", snapshot, error);
        });
    }

    private <E, ID> CompletableFuture<Boolean> postSaveRepair(Snapshot<E, ID> snapshot) {
        log.trace("Running post-save old snapshot removal for snapshot {}", snapshot);
        Class<E> entity = snapshot.getEntityType().getSymbol();
        val historyLength = registry.<E, ID>getDescriptor(entity).getConfiguration().getSnapshotHistoryLength();
        if (!storage.supportsRemoval() || historyLength <= 0) {
            log.trace("Snapshots for entity `{}` are not eligible for removal, skipping", entity);
            return CompletableFutures.FALSE;
        }
        val configuration = registry.<E, ID>getDescriptor(entity).getConfiguration();
        val candidate = snapshot.getSequenceNumber() - configuration.getSnapshotHistoryLength();
        ID id = snapshot.getEntityId().getSymbol();
        if (candidate < 1) {
            log.trace(
                    "Not enough snapshots in entity `{}` with id `{}` history for read removal, skipping",
                    entity,
                    id
            );
            return CompletableFutures.FALSE;
        }
        log.trace("Removing snapshot #{} for entity `{}` with id `{}`", candidate, entity, id);
        return storage
                .remove(snapshot.getEntityType().getType(), snapshot.getEntityId().getEncoded(), candidate)
                .thenApply(success -> {
                    if (success) {
                        log.trace(
                                "Successfully removed snapshot #{} for entity `{}` with id `{}`",
                                candidate,
                                entity,
                                id
                        );
                    } else {
                        log.info(
                                "Couldn't remove snapshot #{} for entity `{}` with id `{}`, but this can be " +
                                        "result of race between processes",
                                candidate,
                                entity,
                                id
                        );
                    }
                    return success;
                });
    }

    public <E, ID> CompletableFuture<Boolean> replace(Snapshot<E, ID> current, Snapshot<E, ID> replacement) {
        log.trace("Replacing {} with {}", current, replacement);
        val future = CompletableFutures
                .execute(() -> {
                    SerializedSnapshot currentSnapshot = serializer.serialize(current);
                    SerializedSnapshot replacementSnapshot = serializer.serialize(replacement);
                    return storage.replace(currentSnapshot, replacementSnapshot);
                })
                .thenCompose(Function.identity());
        return CompletableFutures.onError(future, error -> {
            log.error("Error during replacing snapshot {} with {}: {}", current, replacement, error);
        });
    }

    public <E, ID> CompletableFuture<Boolean> remove(Class<E> entity, ID id, long sequenceNumber) {
        log.trace("Removing snapshot #{} for entity `{}` with id `{}`", sequenceNumber, entity, id);
        val descriptor = registry.<E, ID>getDescriptor(entity);
        val type = descriptor.getType().getType();
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        val future = storage.remove(type, encodedId, sequenceNumber);
        return CompletableFutures.onError(future, error -> {
            log.error(
                    "Error during snapshot #{} for entity `{}` with id `{}` removal: {}",
                    sequenceNumber,
                    entity,
                    id,
                    error
            );
        });
    }

    public <E, ID> CompletableFuture<Optional<Long>> purge(Class<E> entity, ID id) {
        log.trace("Purging snapshots for entity `{}` with id `{}`");
        val descriptor = registry.<E, ID>getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        val future = storage.purge(descriptor.getType().getType(), encodedId);
        return CompletableFutures.onError(future, error -> {
            log.error("Error during snapshot purge for entity `{}` with id `{}`: {}", entity, id, error);
        });
    }
}
