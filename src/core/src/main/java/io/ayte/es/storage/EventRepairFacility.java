package io.ayte.es.storage;

import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.v1.DependencyInjector;
import io.ayte.es.api.v1.DeprecatedMutation;
import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.MutationType;
import io.ayte.es.api.v1.exception.IllegalMutationUpgradeException;
import io.ayte.es.utility.CompletableFutures;
import io.ayte.es.utility.ProbabilityArbiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"squid:S00119"})
public class EventRepairFacility {
    private static final String WARNING_SUFFIX = "While this doesn't precisely indicate an error, read repair " +
            "mechanism has been created only to advance versions of specific mutation type, and you're probably using " +
            "it wrong";
    private static final String TYPE_MISMATCH_WARNING = "Mutation for upgraded {} has different type compared to " +
            "original {}. " + WARNING_SUFFIX;
    private static final String DOWNSHIFTING_VERSION_WARNING = "Mutation for upgraded {} has version number lower " +
            "than original {}. " + WARNING_SUFFIX;
    public static final String CAS_FAIL_DEBUG_MESSAGE = "Failed to perform {} -> {} replacement. This doesn't " +
            "necessarily denote an error, probably CAS swap failed due to high contention";

    private final DescriptorRegistry registry;
    private final EventStorageAdapter storage;
    private final EventSerializer serializer;
    private final ProbabilityArbiter probabilityArbiter;
    private final DependencyInjector injector;

    @SuppressWarnings({"unchecked"})
    public <E, ID> CompletableFuture<Event<E, ID>> process(Event<E, ID> event) {
        if (!decide(event)) {
            return CompletableFutures.completed(event);
        }
        log.debug("Performing read repair for {}", event);
        val mutation = (DeprecatedMutation<E, ID>) event.getMutation();
        val descriptor = registry.<E, ID>getDescriptor(event.getEntityType().getSymbol());
        val process = mutation
                .upgrade(injector)
                .thenApply(result -> {
                    MutationType<?, E, ID> type = descriptor.getMutationType(result.getClass());
                    return event.toBuilder().mutation(result).mutationType(type).build();
                })
                .thenApply(result -> validate(event, result))
                .thenCompose(result -> replace(event, result));
        return CompletableFutures.rescue(process, error -> {
            if (descriptor.getConfiguration().isSuppressRepairErrors()) {
                log.warn("Caught error during read repair for {}, suppressing as configured", event, error);
                return CompletableFutures.completed(event);
            }
            log.error("Read repair for {} has resulted in error", event);
            throw error;
        });
    }

    private <E, ID> CompletableFuture<Event<E, ID>> replace(Event<E, ID> original, Event<E, ID> replacement) {
        return CompletableFutures
                .execute(() -> {
                    val current = serializer.serialize(original);
                    val update = serializer.serialize(replacement);
                    log.debug("Replacing {} with {}", original, replacement);
                    return storage.replace(current, update);
                })
                .thenCompose(Function.identity())
                .thenApply(success -> {
                    if (success) {
                        log.debug("Successfully performed {} -> {} replacement", original, replacement);
                        return replacement;
                    }
                    log.debug(CAS_FAIL_DEBUG_MESSAGE);
                    return original;
                });
    }

    private <E, ID> Event<E, ID> validate(Event<E, ID> original, Event<E, ID> upgraded) {
        val originalType = original.getMutationType();
        val upgradedType = upgraded.getMutationType();
        if (MutationType.equalStoragePrint(originalType, upgradedType)) {
            val message = "Storage print for original " + originalType + " and upgraded " + upgradedType +
                    " mutation types is equal";
            throw new IllegalMutationUpgradeException(message);
        }
        if (!originalType.getType().equals(upgradedType.getType())) {
            log.warn(TYPE_MISMATCH_WARNING, original, upgraded);
        } else if (originalType.getVersion() > upgradedType.getVersion()) {
            log.warn(DOWNSHIFTING_VERSION_WARNING, original, upgraded);
        }
        return upgraded;
    }

    public <E, ID> boolean eligible(Event<E, ID> event) {
        return storage.supportsReplace() && event.getMutation() instanceof DeprecatedMutation;
    }

    private <E, ID> boolean decide(Event<E, ID> event) {
        if (!storage.supportsReplace()) {
            return false;
        }
        val mutation = event.getMutation();
        if (!(mutation instanceof DeprecatedMutation)) {
            return false;
        }
        val descriptor = registry.getDescriptor(event.getEntityType().getSymbol());
        return probabilityArbiter.decide(descriptor.getConfiguration().getEventRepairProbability());
    }
}
