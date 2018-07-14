package io.ayte.es.api.v1;

import io.ayte.es.api.v1.exception.MissingVersionException;
import io.ayte.es.api.v1.misc.Pair;
import io.ayte.es.api.v1.misc.Result;
import io.ayte.es.utility.CompletableFutures;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This class (along with {@link Repository}) provides main interface for this
 * library. It is implied that after setup all operations go either through
 * Director or {@link Repository}.
 */
@SuppressWarnings("squid:S00119")
public interface Director {
    /**
     * @return Current highest event number for entity. If entity doesn't have
     * stored events, returns empty optional.
     */
    <E, ID> CompletableFuture<Optional<Long>> getSequenceNumber(Class<E> entity, ID id);

    /**
     * @return Entity at specified version or {@link MissingVersionException} if
     * such version doesn't exist
     */
    <E, ID> CompletableFuture<Optional<Entity<E, ID>>> get(Class<E> entity, ID id, long version);

    /**
     * Tries to save provided mutation as event under specified event number.
     *
     * @return True on success, false if event number is already occupied
     */
    <E, ID> CompletableFuture<Boolean> save(SaveRequest<E, ID> request);

    <E, ID> CompletableFuture<List<Event<E, ID>>> getEvents(Class<E> entity, ID id, long skip, long size);
    <E, ID> CompletableFuture<List<Snapshot<E, ID>>> getSnapshots(Class<E> entity, ID id, long skip, long size);
    <E, ID> CompletableFuture<Optional<Snapshot<E, ID>>> getSnapshot(Class<E> entity, ID id, long snapshotNumber);

    /**
     * Purges events and snapshots. Requires underlying event and snapshot
     * storages to support purging.
     *
     * @return Pair of amount of deleted events and snapshots.
     */
    <E, ID> CompletableFuture<Pair<Optional<Long>, Optional<Long>>> purge(Class<E> entity, ID id);

    /**
     * Deletes specified snapshot. Corresponding operation has to be supported
     * by snapshot storage.
     *
     * @return Operation success
     */
    <E, ID> CompletableFuture<Boolean> deleteSnapshot(Class<E> entity, ID id, long snapshotNumber);

    <E, ID> Repository<E, ID> getRepository(Class<E> entity);

    /**
     * @return Entity at current state or empty optional
     */
    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    default <E, ID> CompletableFuture<Optional<Entity<E, ID>>> get(Class<E> entity, ID id) {
        return getSequenceNumber(entity, id)
                .thenCompose(number -> {
                    return number
                            .map(value -> get(entity, id, value))
                            .orElseGet(CompletableFutures::emptyOptional);
                });
    }

    /**
     * Tries to save mutation determining event number on it's own. Requires
     * extra storage RTT compared to {@link #save(SaveRequest)}
     */
    default <E, ID> CompletableFuture<Boolean> save(AppendRequest<E, ID> request) {
        return getSequenceNumber(request.getEntity(), request.getId())
                .thenCompose(number -> save(SaveRequest.from(request, number.orElse(0L) + 1)));
    }

    /**
     * Tries to save provided mutation, returning entity with mutation already
     * applied.
     *
     * Please note that this method requires extra storage RTT compared to
     * {@link #save(AppendRequest)}.
     *
     * @return Result with entity with applied mutation or unsuccessful result.
     */
    default <E, ID> CompletableFuture<Result<Entity<E, ID>>> apply(SaveRequest<E, ID> request) {
        return save(request)
                .thenCompose(success -> {
                    if (!success) {
                        return CompletableFutures.completed(Result.failure());
                    }
                    return get(request.getEntity(), request.getId(), request.getSequenceNumber())
                            .thenApply(Result::from);
                });
    }

    /**
     * Same as {@link #apply(SaveRequest)}, but determines
     * event number on it's own. Requires extra RTT to storage, resulting in 3
     * RTTs total.
     */
    default <E, ID> CompletableFuture<Result<Entity<E, ID>>> apply(AppendRequest<E, ID> request) {
        return getSequenceNumber(request.getEntity(), request.getId())
                .thenCompose(number -> apply(SaveRequest.from(request, number.orElse(0L) + 1)));
    }

    default <E, ID> CompletableFuture<List<Event<E, ID>>> getEvents(Class<E> entity, ID id, long skip) {
        return getEvents(entity, id, skip, Long.MAX_VALUE);
    }

    default <E, ID> CompletableFuture<List<Event<E, ID>>> getEvents(Class<E> entity, ID id) {
        return getEvents(entity, id, 0);
    }

    default <E, ID> CompletableFuture<List<Snapshot<E, ID>>> getSnapshots(Class<E> entity, ID id, long skip) {
        return getSnapshots(entity, id, skip, Long.MAX_VALUE);
    }

    default <E, ID> CompletableFuture<List<Snapshot<E, ID>>> getSnapshots(Class<E> entity, ID id) {
        return getSnapshots(entity, id, 0);
    }

    default <E, ID> CompletableFuture<Boolean> exists(Class<E> entity, ID id) {
        return getSequenceNumber(entity, id).thenApply(Optional::isPresent);
    }
}
