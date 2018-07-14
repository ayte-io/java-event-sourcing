package io.ayte.es.storage;

import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.utility.CompletableFutures;
import io.ayte.es.utility.ProbabilityArbiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"squid:S00119", "squid:S1602", "CodeBlock2Expr"})
public class SnapshotRemovalFacility {
    private final SnapshotStorageAdapter storage;
    private final DescriptorRegistry registry;
    private final ProbabilityArbiter arbiter;

    public <E, ID> CompletableFuture<Optional<Snapshot<E, ID>>> process(Snapshot<E, ID> snapshot) {
        val entity = snapshot.getEntityType().getSymbol();
        val id = snapshot.getEntityId().getSymbol();
        val sequenceNumber = snapshot.getSequenceNumber();
        return process(entity, id, sequenceNumber)
                .thenApply(removed -> removed ? Optional.empty() : Optional.of(snapshot));
    }

    public <E, ID> CompletableFuture<Boolean> process(Class<E> entity, ID id, long sequenceNumber) {
        if (!storage.supportsRemoval()) {
            return CompletableFutures.FALSE;
        }
        val descriptor = registry.<E, ID>getDescriptor(entity);
        val configuration = descriptor.getConfiguration();
        val entityId = descriptor.getIdentifierConverter().encode(id);
        val entityType = descriptor.getType().getType();
        long historyLength = configuration.getSnapshotHistoryLength();
        if (historyLength <= 0 || !arbiter.decide(configuration.getSnapshotCleanupProbability())) {
            return CompletableFutures.FALSE;
        }
        val future = storage
                .getSequenceNumber(entityType, entityId)
                .thenCompose(container -> {
                    return container
                            .filter(number -> sequenceNumber + historyLength <= number)
                            .map(snapshot -> storage.remove(entityType, entityId, sequenceNumber))
                            .orElse(CompletableFutures.FALSE);
                });
        return CompletableFutures.rescue(future, error -> {
            if (configuration.isSuppressRepairErrors()) {
                log.error(
                        "Error during #{} snapshot removal for entity `{}` with id `{}`, suppressed as configured",
                        sequenceNumber,
                        entity,
                        id,
                        error
                );
                return CompletableFutures.FALSE;
            }
            log.error(
                    "Error during #{} snapshot removal for entity `{}` with id `{}`: {}",
                    sequenceNumber,
                    entity,
                    id,
                    error
            );
            throw error;
        });
    }

    public <E, ID> boolean eligible(Snapshot<E, ID> snapshot) {
        return eligible(snapshot.getEntityType().getSymbol());
    }

    public <E, ID> boolean eligible(Class<E> entity) {
        if (!storage.supportsRemoval()) {
            return false;
        }
        val configuration = registry.<E, ID>getDescriptor(entity).getConfiguration();
        return configuration.getSnapshotHistoryLength() > 0 && configuration.getSnapshotCleanupProbability() > 0;
    }
}
