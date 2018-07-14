package io.ayte.es.test;

import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.internal.EntityConfiguration;
import io.ayte.es.api.internal.EntityDescriptor;
import io.ayte.es.api.v1.DependencyInjector;
import io.ayte.es.api.v1.DeprecatedMutation;
import io.ayte.es.api.v1.EntityType;
import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.Identifier;
import io.ayte.es.api.v1.IdentifierConverter;
import io.ayte.es.api.v1.Mutation;
import io.ayte.es.api.v1.MutationType;
import io.ayte.es.api.v1.SaveRequest;
import io.ayte.es.api.v1.Serializer;
import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.api.v1.TimeSource;
import io.ayte.es.api.v1.storage.SerializedEvent;
import io.ayte.es.api.v1.storage.SerializedSnapshot;
import io.ayte.es.storage.EventSerializer;
import io.ayte.es.storage.EventStorageAdapter;
import io.ayte.es.storage.SnapshotRemovalFacility;
import io.ayte.es.storage.SnapshotSerializer;
import io.ayte.es.storage.SnapshotStorageAdapter;
import io.ayte.es.utility.CompletableFutures;
import io.ayte.es.utility.ProbabilityArbiter;
import org.junit.jupiter.api.BeforeEach;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract public class SimpleInfrastructureTestBase {
    protected static final EntityType<Integer> ENTITY_TYPE = EntityType
            .<Integer>builder()
            .type("integer")
            .symbol(Integer.class)
            .build();
    protected static final IdentifierConverter<Integer> CONVERTER = new IdentifierConverter<Integer>() {
        @Override
        public String encode(Integer integer) {
            return integer.toString();
        }

        @Override
        public Integer decode(String encoded) {
            return Integer.valueOf(encoded);
        }
    };
    protected static final Identifier<Integer> ENTITY_ID = new Identifier<>(1, CONVERTER.encode(1));
    protected static final byte[] PAYLOAD = new byte[0];
    protected static final Integer ENTITY = 1;

    protected TimeSource timeSource;
    protected EntityDescriptor<Integer, Integer> descriptor;
    protected DescriptorRegistry registry;
    protected Serializer serializer;
    protected EventSerializer eventSerializer;
    protected EventStorageAdapter eventStorageAdapter;
    protected SnapshotSerializer snapshotSerializer;
    protected SnapshotStorageAdapter snapshotStorageAdapter;
    protected SnapshotRemovalFacility snapshotRemovalFacility;
    protected ProbabilityArbiter probabilityArbiter;

    protected SerializedEvent deprecatedSerializedEvent1;
    protected SerializedEvent serializedEvent1;
    protected SerializedEvent serializedEvent2;
    protected Mutation<Integer, Integer> deprecatedMutation;
    protected Mutation<Integer, Integer> mutationA;
    protected Mutation<Integer, Integer> mutationB;
    protected MutationType<Mutation<Integer, Integer>, Integer, Integer> deprecatedMutationType;
    protected MutationType<Mutation<Integer, Integer>, Integer, Integer> mutationTypeA;
    protected MutationType<Mutation<Integer, Integer>, Integer, Integer> mutationTypeB;
    protected Event<Integer, Integer> deprecatedEvent1;
    protected Event<Integer, Integer> event1;
    protected Event<Integer, Integer> event2;
    protected SaveRequest<Integer, Integer> saveRequest1;
    protected SaveRequest<Integer, Integer> saveRequest2;
    protected Snapshot<Integer, Integer> snapshot1;
    protected Snapshot<Integer, Integer> snapshot2;
    protected SerializedSnapshot serializedSnapshot1;
    protected SerializedSnapshot serializedSnapshot2;

    @BeforeEach
    @SuppressWarnings({"unchecked"})
    public void setUp() throws Exception {
        timeSource = mock(TimeSource.class);
        when(timeSource.getCurrentTime()).thenReturn(ZonedDateTime.now());

        probabilityArbiter = mock(ProbabilityArbiter.class);
        when(probabilityArbiter.decide(anyDouble())).thenReturn(true);

        descriptor = mock(EntityDescriptor.class);
        when(descriptor.getIdentifierConverter()).thenReturn(CONVERTER);
        when(descriptor.getType()).thenReturn(ENTITY_TYPE);
        when(descriptor.getConfiguration()).thenReturn(EntityConfiguration.DEFAULT);

        registry = mock(DescriptorRegistry.class);
        when(registry.<Integer, Integer>getDescriptor(ENTITY_TYPE.getSymbol())).thenReturn(descriptor);
        when(registry.<Integer, Integer>getDescriptor(ENTITY_TYPE.getType())).thenReturn(descriptor);

        serializer = mock(Serializer.class);
        when(serializer.serialize(any())).thenReturn(PAYLOAD);
        
        deprecatedMutation = new DeprecatedMutation<Integer, Integer>() {
            @Override
            public CompletableFuture<Mutation<Integer, Integer>> upgrade(DependencyInjector injector) {
                return CompletableFutures.completed(mutationA);
            }

            @Override
            public Integer apply(Integer entity, Context<Integer, Integer> context) {
                return entity + 1;
            }
        };
        deprecatedMutationType = MutationType.<Mutation<Integer,Integer>, Integer, Integer>builder()
                .symbol((Class) deprecatedMutation.getClass())
                .type("increment")
                .version(1)
                .build();
        when(descriptor.getMutationType(deprecatedMutationType.getSymbol())).thenReturn(deprecatedMutationType);
        when(descriptor.getMutationType(deprecatedMutationType.getType(), deprecatedMutationType.getVersion()))
                .thenReturn(deprecatedMutationType);
        when(serializer.deserialize(PAYLOAD, deprecatedMutationType.getSymbol())).thenReturn(deprecatedMutation);

        mutationA = (entity, context) -> entity + 1;
        mutationTypeA = MutationType.<Mutation<Integer,Integer>, Integer, Integer>builder()
                .symbol((Class) mutationA.getClass())
                .type("increment")
                .version(2)
                .build();
        when(descriptor.getMutationType(mutationTypeA.getSymbol())).thenReturn(mutationTypeA);
        when(descriptor.getMutationType(mutationTypeA.getType(), mutationTypeA.getVersion())).thenReturn(mutationTypeA);
        when(serializer.deserialize(PAYLOAD, mutationTypeA.getSymbol())).thenReturn(mutationA);
        when(serializer.deserialize(PAYLOAD, Integer.class)).thenReturn(ENTITY);

        mutationB = (entity, context) -> entity - 1;
        mutationTypeB = MutationType.<Mutation<Integer,Integer>, Integer, Integer>builder()
                .symbol((Class) mutationB.getClass())
                .type("decrement")
                .version(1)
                .build();
        when(descriptor.getMutationType(mutationTypeB.getSymbol())).thenReturn(mutationTypeB);
        when(descriptor.getMutationType(mutationTypeB.getType(), mutationTypeB.getVersion())).thenReturn(mutationTypeB);
        when(serializer.deserialize(PAYLOAD, mutationTypeB.getSymbol())).thenReturn(mutationB);

        deprecatedSerializedEvent1 = makeSerializedEvent(deprecatedMutationType, 1);
        serializedEvent1 = makeSerializedEvent(mutationTypeA, 1);
        serializedEvent2 = makeSerializedEvent(mutationTypeB, 2);

        deprecatedEvent1 = makeEvent(deprecatedMutation, deprecatedMutationType, 1);
        event1 = makeEvent(mutationA, mutationTypeA, 1);
        event2 = makeEvent(mutationB, mutationTypeB, 2);

        saveRequest1 = makeRequest(mutationA, 1);
        saveRequest2 = makeRequest(mutationB, 2);

        snapshot1 = makeSnapshot(1, 1);
        snapshot2 = makeSnapshot(1, 2);

        serializedSnapshot1 = makeSerializedSnapshot(1, 1);
        serializedSnapshot2 = makeSerializedSnapshot(1, 2);

        eventSerializer = mock(EventSerializer.class);
        when(eventSerializer.<Integer, Integer>deserialize(serializedEvent1)).thenReturn(event1);
        when(eventSerializer.<Integer, Integer>deserialize(serializedEvent2)).thenReturn(event2);
        when(eventSerializer.<Integer, Integer>deserialize(deprecatedSerializedEvent1)).thenReturn(deprecatedEvent1);
        when(eventSerializer.serialize(event1)).thenReturn(serializedEvent1);
        when(eventSerializer.serialize(event2)).thenReturn(serializedEvent2);
        when(eventSerializer.serialize(deprecatedEvent1)).thenReturn(deprecatedSerializedEvent1);
        when(eventSerializer.serialize(saveRequest1)).thenReturn(serializedEvent1);
        when(eventSerializer.serialize(saveRequest2)).thenReturn(serializedEvent2);
        when(eventSerializer.deserializeAll(any())).thenCallRealMethod();

        snapshotSerializer = mock(SnapshotSerializer.class);
        when(snapshotSerializer.<Integer, Integer>deserialize(serializedSnapshot1)).thenReturn(snapshot1);
        when(snapshotSerializer.<Integer, Integer>deserialize(serializedSnapshot2)).thenReturn(snapshot2);
        when(snapshotSerializer.serialize(snapshot1)).thenReturn(serializedSnapshot1);
        when(snapshotSerializer.serialize(snapshot2)).thenReturn(serializedSnapshot2);
        when(snapshotSerializer.deserializeAll(any())).thenCallRealMethod();

        eventStorageAdapter = mock(EventStorageAdapter.class);
        when(eventStorageAdapter.getSequenceNumber(any(), any())).thenReturn(CompletableFutures.emptyOptional());
        when(eventStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.optional(2L));
        when(eventStorageAdapter.get(any(), any(), anyLong())).thenReturn(CompletableFutures.emptyOptional());
        when(eventStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.optional(serializedEvent1));
        when(eventStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 2))
                .thenReturn(CompletableFutures.optional(serializedEvent2));
        when(eventStorageAdapter.list(any(), any(), anyLong(), anyLong()))
                .thenReturn(CompletableFutures.completed(Collections.emptyList()));
        when(eventStorageAdapter.list(eq(ENTITY_TYPE.getType()), eq(ENTITY_ID.getEncoded()), anyLong(), anyLong()))
                .then(input -> {
                    long skip = input.getArgument(2);
                    long limit = input.getArgument(3);
                    List<SerializedEvent> events = Stream.of(serializedEvent1, serializedEvent2)
                            .skip(skip)
                            .limit(limit)
                            .collect(Collectors.toList());
                    return CompletableFutures.completed(events);
                });
        when(eventStorageAdapter.supportsReplace()).thenReturn(true);
        when(eventStorageAdapter.replace(any(), any())).thenReturn(CompletableFutures.TRUE);
        when(eventStorageAdapter.supportsPurge()).thenReturn(true);
        when(eventStorageAdapter.purge(any(), any())).thenReturn(CompletableFutures.optional(0L));

        snapshotStorageAdapter = mock(SnapshotStorageAdapter.class);
        when(snapshotStorageAdapter.supportsRemoval()).thenReturn(true);
        when(snapshotStorageAdapter.supportsReplacement()).thenReturn(true);
        when(snapshotStorageAdapter.supportsPurge()).thenReturn(true);
        when(snapshotStorageAdapter.remove(any(), any(), anyLong())).thenReturn(CompletableFutures.TRUE);
        when(snapshotStorageAdapter.purge(any(), any())).thenReturn(CompletableFutures.emptyOptional());
        when(snapshotStorageAdapter.replace(any(), any())).thenReturn(CompletableFutures.TRUE);
        when(snapshotStorageAdapter.getSequenceNumber(any(), any())).thenReturn(CompletableFutures.emptyOptional());
        when(snapshotStorageAdapter.save(any())).thenReturn(CompletableFutures.TRUE);
        when(snapshotStorageAdapter.get(any(), any(), anyLong())).thenReturn(CompletableFutures.emptyOptional());
        when(snapshotStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.optional(serializedSnapshot1));
        when(snapshotStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 2))
                .thenReturn(CompletableFutures.optional(serializedSnapshot2));
        when(snapshotStorageAdapter.getLast(any(), any()))
                .thenReturn(CompletableFutures.optional(serializedSnapshot2));
        when(snapshotStorageAdapter.getLast(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.optional(serializedSnapshot2));

        snapshotRemovalFacility = mock(SnapshotRemovalFacility.class);
        when(snapshotRemovalFacility.eligible(any(Snapshot.class))).thenReturn(true);
        when(snapshotRemovalFacility.eligible(any(Class.class))).thenReturn(true);
        when(snapshotRemovalFacility.process(any())).then(input -> CompletableFutures.optional(input.getArgument(0)));
        when(snapshotRemovalFacility.process(any(), any(), anyLong())).thenReturn(CompletableFutures.TRUE);
    }

    protected SerializedEvent makeSerializedEvent(MutationType<?, Integer, Integer> type, long sequenceNumber) {
        return SerializedEvent.builder()
                .entityType(ENTITY_TYPE.getType())
                .entityId(ENTITY_ID.getEncoded())
                .sequenceNumber(sequenceNumber)
                .mutationType(type.getType())
                .mutationVersion(type.getVersion())
                .payload(PAYLOAD)
                .acknowledgedAt(timeSource.getCurrentTime())
                .metadata(Collections.emptyMap())
                .build();
    }

    protected Event<Integer, Integer> makeEvent(
            Mutation<Integer, Integer> mutation,
            MutationType<?, Integer, Integer> mutationType,
            long sequenceNumber
    ) {
        return Event.<Integer, Integer>builder()
                .entityType(ENTITY_TYPE)
                .entityId(ENTITY_ID)
                .sequenceNumber(sequenceNumber)
                .mutationType(mutationType)
                .mutation(mutation)
                .acknowledgedAt(timeSource.getCurrentTime())
                .metadata(Collections.emptyMap())
                .build();
    }

    protected SaveRequest<Integer, Integer> makeRequest(Mutation<Integer, Integer> mutation, long sequenceNumber) {
        return SaveRequest.<Integer, Integer>builder()
                .entity(ENTITY_TYPE.getSymbol())
                .id(ENTITY_ID.getSymbol())
                .sequenceNumber(sequenceNumber)
                .mutation(mutation)
                .metadata(Collections.emptyMap())
                .build();
    }

    protected Snapshot<Integer, Integer> makeSnapshot(long sequenceNumber, long entityVersion) {
        return Snapshot.<Integer, Integer>builder()
                .entityType(ENTITY_TYPE)
                .entityId(ENTITY_ID)
                .sequenceNumber(sequenceNumber)
                .entityVersion(entityVersion)
                .entity(ENTITY)
                .eventAcknowledgedAt(timeSource.getCurrentTime())
                .eventOccurredAt(timeSource.getCurrentTime())
                .entityCreatedAt(timeSource.getCurrentTime())
                .createdAt(timeSource.getCurrentTime())
                .metadata(Collections.emptyMap())
                .build();
    }

    protected SerializedSnapshot makeSerializedSnapshot(long sequenceNumber, long entityVersion) {
        return SerializedSnapshot.builder()
                .entityType(ENTITY_TYPE.getType())
                .entityId(ENTITY_ID.getEncoded())
                .sequenceNumber(sequenceNumber)
                .entityVersion(entityVersion)
                .payload(PAYLOAD)
                .eventAcknowledgedAt(timeSource.getCurrentTime())
                .eventOccurredAt(timeSource.getCurrentTime())
                .entityCreatedAt(timeSource.getCurrentTime())
                .createdAt(timeSource.getCurrentTime())
                .metadata(Collections.emptyMap())
                .build();
    }
}
