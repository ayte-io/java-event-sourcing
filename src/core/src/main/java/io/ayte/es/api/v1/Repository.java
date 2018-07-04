package io.ayte.es.api.v1;

import io.ayte.es.api.v1.misc.Pair;
import io.ayte.es.api.v1.misc.Result;
import io.ayte.es.utility.CompletableFutures;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @see Director for corresponding method clarification
 * @param <E> Entity type
 * @param <ID> Identifier type
 */
@SuppressWarnings("squid:S00119")
public interface Repository<E, ID> {
    CompletableFuture<Entity<E, ID>> get(ID id, long version);
    CompletableFuture<Boolean> save(ID id, Mutation<E, ID> mutation, long sequenceNumber);
    CompletableFuture<Optional<Long>> getSequenceNumber(ID id);
    CompletableFuture<List<Event<?, E, ID>>> getEvents(ID id, long skip, long limit);
    CompletableFuture<Pair<Optional<Long>, Optional<Long>>> purge(ID id);

    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    default CompletableFuture<Optional<Entity<E, ID>>> get(ID id) {
        return getSequenceNumber(id)
                .thenCompose(number -> {
                    return number
                            .map(value -> get(id, value).thenApply(Optional::of))
                            .orElseGet(CompletableFutures::emptyOptional);
                });
    }
    default CompletableFuture<Boolean> exists(ID id) {
        return getSequenceNumber(id).thenApply(Optional::isPresent);
    }

    default CompletableFuture<Boolean> save(ID id, Mutation<E, ID> mutation) {
        return getSequenceNumber(id).thenCompose(number -> save(id, mutation, number.orElse(0L) + 1));
    }

    default CompletableFuture<Result<Entity<E, ID>>> apply(ID id, Mutation<E, ID> mutation, long sequenceNumber) {
        return save(id, mutation, sequenceNumber)
                .thenCompose(success -> {
                    if (!success) {
                        return CompletableFutures.completed(Result.failure());
                    }
                    return get(id, sequenceNumber).thenApply(Result::successful);
                });
    }

    default CompletableFuture<Result<Entity<E, ID>>> apply(ID id, Mutation<E, ID> mutation) {
        return getSequenceNumber(id).thenCompose(number -> apply(id, mutation, number.orElse(0L) + 1));
    }

    default CompletableFuture<List<Event<?, E, ID>>> getEvents(ID id, long skip) {
        return getEvents(id, skip, Long.MAX_VALUE);
    }

    default CompletableFuture<List<Event<?, E, ID>>> getEvents(ID id) {
        return getEvents(id, 0);
    }
}
