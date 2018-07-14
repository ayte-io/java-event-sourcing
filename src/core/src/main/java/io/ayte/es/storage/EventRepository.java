package io.ayte.es.storage;

import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.internal.EntityDescriptor;
import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.SaveRequest;
import io.ayte.es.utility.CompletableFutures;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@SuppressWarnings("squid:S00119")
public class EventRepository {
    private final DescriptorRegistry registry;
    private final EventStorageAdapter storage;
    private final EventSerializer serializer;
    private final EventRepairFacility readRepairFacility;

    public EventRepository(
            DescriptorRegistry registry,
            EventStorageAdapter storage,
            EventSerializer serializer,
            EventRepairFacility readRepairFacility
    ) {
        this.registry = registry;
        this.storage = storage;
        this.serializer = serializer;
        this.readRepairFacility = readRepairFacility;
    }

    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    public <E, ID> CompletableFuture<Optional<Event<E, ID>>> get(Class<E> entity, ID id, long sequenceNumber) {
        log.trace("Retrieving event #{} for entity `{}` with id {}", sequenceNumber, entity, id);
        EntityDescriptor<E, ID> descriptor = registry.getDescriptor(entity);
        String encodedId = descriptor.getIdentifierConverter().encode(id);
        CompletableFuture<Optional<Event<E, ID>>> result = this
                .storage
                .get(descriptor.getType().getType(), encodedId, sequenceNumber)
                .thenCompose(container -> {
                    return container
                            .map(event -> CompletableFutures.execute(() -> serializer.<E, ID>deserialize(event)))
                            .orElseGet(CompletableFutures::nullFuture);
                })
                .thenCompose(readRepairFacility::process)
                .thenApply(Optional::ofNullable);
        return CompletableFutures.onError(result, error -> {
            log.error(
                    "Error during #{} event retrieval for entity `{}` with id `{}`: {}",
                    sequenceNumber,
                    entity,
                    id,
                    error
            );
        });
    }

    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    public <E, ID> CompletableFuture<List<Event<E, ID>>> list(Class<E> entity, ID id, long skip, long limit) {
        EntityDescriptor<E, ID> descriptor = registry.getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        CompletableFuture<List<Event<E, ID>>> fetch = this
                .storage
                .list(descriptor.getType().getType(), encodedId, skip, limit)
                .thenCompose(events -> CompletableFutures.execute(() -> serializer.deserializeAll(events)));
        double repairProbability = descriptor.getConfiguration().getEventRepairProbability();
        val future = repairProbability > 0 ? fetch.thenCompose(this::repair) : fetch;
        return CompletableFutures.onError(future, error -> {
            log.error(
                    "Error during event list retrieval for entity `{}` with id `{}` (skip {}, limit {}): {}",
                    entity,
                    id,
                    skip,
                    limit,
                    error
            );
        });
    }

    private <E, ID> CompletableFuture<List<Event<E, ID>>> repair(List<Event<E, ID>> events) {
        var cursor = 0;
        log.trace("Running read repair checks for event list");
        for (val event : events) {
            if (readRepairFacility.eligible(event)) {
                log.debug("Found read repair-eligible event {} during list fetch", event);
                val index = cursor;
                // error is reported by calling code
                return readRepairFacility
                        .process(event)
                        .thenApply(processed -> {
                            val result = new ArrayList<Event<E, ID>>(events);
                            result.set(index, processed);
                            return result;
                        });
            }
            cursor++;
        }
        log.trace("Did not find any read repair-eligible events");
        return CompletableFutures.completed(events);
    }

    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    public <E, ID> CompletableFuture<Boolean> save(SaveRequest<E, ID> request) {
        log.trace("Saving {}", request);
        val future = CompletableFutures
                .execute(() -> serializer.serialize(request))
                .thenCompose(storage::save);
        return CompletableFutures.onError(future, error -> {
            log.error("Error during {} save: {}", request, error);
        });
    }

    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    public <E, ID> CompletableFuture<Optional<Long>> getSequenceNumber(Class<E> entity, ID id) {
        log.trace("Retrieving current sequence number for entity `{}` with id `{}`", entity, id);
        EntityDescriptor<E, ID> descriptor = registry.getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        val future = storage.getSequenceNumber(descriptor.getType().getType(), encodedId);
        return CompletableFutures.onError(future, error -> {
            log.error("Error during entity `{}` with id `{}` current sequence number retrieval: {}", entity, id, error);
        });
    }

    @SuppressWarnings({"squid:S1602", "CodeBlock2Expr"})
    public <E, ID> CompletableFuture<Optional<Long>> purge(Class<E> entity, ID id) {
        EntityDescriptor<E, ID> descriptor = registry.getDescriptor(entity);
        val encodedId = descriptor.getIdentifierConverter().encode(id);
        log.trace("Purging entity `{}` with id `{}`", entity, id);
        val future = storage
                .purge(descriptor.getType().getType(), encodedId)
                .thenApply(result -> {
                    log.trace("Purged entity `{}` with id `{}`", entity, id);
                    return result;
                });
        return CompletableFutures.onError(future, error -> {
            log.error("Error during purging entity `{}` with id `{}`: {}", entity, id, error);
        });
    }
}
