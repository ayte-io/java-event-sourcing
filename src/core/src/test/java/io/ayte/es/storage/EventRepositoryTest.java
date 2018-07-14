package io.ayte.es.storage;

import io.ayte.es.api.internal.EntityConfiguration;
import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.storage.SerializedEvent;
import io.ayte.es.test.SimpleInfrastructureTestBase;
import io.ayte.es.utility.CompletableFutures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageCompletedWithExceptionThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageCompletedWithValueThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithExceptionThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithValueThat;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static io.ayte.es.test.matchers.EventMatcher.equalToEvent;
import static io.ayte.es.test.matchers.EventMatcher.equalToEvents;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class EventRepositoryTest extends SimpleInfrastructureTestBase {
    private EventRepairFacility readRepairFacility;
    private EventRepository sut;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        readRepairFacility = mock(EventRepairFacility.class);
        when(readRepairFacility.process(any())).then(input -> CompletableFutures.completed(input.getArgument(0)));
        
        sut = new EventRepository(
                registry,
                eventStorageAdapter,
                eventSerializer,
                readRepairFacility
        );
    }

    @Test
    public void getSuccessfullyRetrievesSingleEvent() {
        CompletableFuture<Optional<Event<Integer, Integer>>> future = sut
                .get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageWillCompleteWithValueThat(optionalWithValue(equalToEvent(event1))));
    }

    @Test
    public void getPassesCompletableFutureWrappedException() throws Exception {
        Throwable expectation = new IOException();
        when(eventStorageAdapter.get(any(), any(), anyLong())).thenReturn(CompletableFutures.exceptional(expectation));
        CompletableFuture<Optional<Event<Integer, Integer>>> future = sut
                .get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageCompletedWithExceptionThat(sameInstance(expectation)));
    }

    @Test
    public void getFillsMissingMetadata() throws Exception {
        SerializedEvent serializedEvent = serializedEvent1.toBuilder().metadata(null).build();
        when(eventStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.completed(Optional.of(serializedEvent)));
        Optional<Event<Integer, Integer>> container = sut
                .get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1)
                .get();
        assertTrue(container.isPresent());
        Event<Integer, Integer> event = container.get();
        assertEquals(event.getEntityType(), ENTITY_TYPE);
        assertEquals(event.getEntityId(), ENTITY_ID);
        assertNotNull(event.getMetadata());
    }

    @Test
    public void getPerformsReadRepair() throws Exception {
        EntityConfiguration configuration = EntityConfiguration.builder()
                .eventRepairProbability(1.0)
                .build();

        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(eventSerializer.<Integer, Integer>deserialize(deprecatedSerializedEvent1))
                .thenReturn(deprecatedEvent1);

        when(eventStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.optional(deprecatedSerializedEvent1));

        when(readRepairFacility.process(deprecatedEvent1))
                .thenReturn(CompletableFutures.completed(event1));

        CompletableFuture<Optional<Event<Integer, Integer>>> future = sut
                .get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageWillCompleteWithValueThat(optionalWithValue(equalToEvent(event1))));
        verify(readRepairFacility, times(1)).process(any());
    }

    @Test
    public void listCorrectlyReads() {
        List<Event<Integer, Integer>> expectation = Arrays.asList(event1, event2);
        CompletableFuture<List<Event<Integer, Integer>>> future = sut
                .list(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 0, 2);
        assertThat(future, stageWillCompleteWithValueThat(equalToEvents(expectation)));
        verify(eventStorageAdapter, times(1)).list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2);
    }

    @Test
    public void listPassesExceptionThrough() {
        Throwable exception = new RuntimeException();
        when(eventStorageAdapter.list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2))
                .thenReturn(CompletableFutures.exceptional(exception));
        CompletableFuture<List<Event<Integer, Integer>>> result = sut
                .list(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 0, 2);
        assertThat(result, stageWillCompleteWithExceptionThat(equalTo(exception)));
        verify(eventStorageAdapter, times(1)).list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2);
    }

    @Test
    public void listPerformsReadRepair() throws Exception {
        EntityConfiguration configuration = EntityConfiguration.builder()
                .eventRepairProbability(1.0)
                .build();

        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(eventStorageAdapter.list(any(), any(), anyLong(), anyLong()))
                .thenReturn(CompletableFutures.completed(Arrays.asList(deprecatedSerializedEvent1, serializedEvent2)));

        when(readRepairFacility.eligible(any()))
                .thenReturn(true);

        when(readRepairFacility.process(deprecatedEvent1))
                .thenReturn(CompletableFutures.completed(event1));

        List<Event<Integer, Integer>> expectation = Arrays.asList(event1, event2);
        CompletableFuture<List<Event<Integer, Integer>>> future = sut
                .list(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 0, 2);

        assertThat(future, stageWillCompleteWithValueThat(equalToEvents(expectation)));
        verify(readRepairFacility, times(1)).process(any());
    }

    @Test
    public void listDoesNotPerformRepairForZeroProbability() throws Exception {
        EntityConfiguration configuration = EntityConfiguration.builder()
                .eventRepairProbability(0)
                .build();

        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(eventStorageAdapter.list(any(), any(), anyLong(), anyLong()))
                .thenReturn(CompletableFutures.completed(Arrays.asList(deprecatedSerializedEvent1, serializedEvent2)));

        when(readRepairFacility.eligible(any()))
                .thenReturn(true);

        when(readRepairFacility.process(deprecatedEvent1))
                .thenReturn(CompletableFutures.completed(event1));

        List<Event<Integer, Integer>> expectation = Arrays.asList(deprecatedEvent1, event2);
        CompletableFuture<List<Event<Integer, Integer>>> future = sut
                .list(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 0, 2);
        assertThat(future, stageWillCompleteWithValueThat(equalToEvents(expectation)));
        verify(readRepairFacility, times(0)).process(any());
    }

    @Test
    public void purgePassesCallThrough() {
        Optional<Long> response = Optional.of(new Random().nextLong());
        when(eventStorageAdapter.purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.completed(response));
        CompletableFuture<Optional<Long>> outcome = sut.purge(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(outcome, stageWillCompleteWithValueThat(equalTo(response)));
        verify(eventStorageAdapter, times(1)).purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
    }

    @Test
    public void purgePassesExceptionThrough() {
        Throwable exception = new UnsupportedOperationException();
        when(eventStorageAdapter.purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.exceptional(exception));
        CompletableFuture<Optional<Long>> outcome = sut.purge(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(outcome, stageCompletedWithExceptionThat(equalTo(exception)));
        verify(eventStorageAdapter, times(1)).purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
    }

    @Test
    public void getSequenceNumberPassesCallThrough() {
        Long result = new Random().nextLong();
        when(eventStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.optional(result));
        CompletableFuture<Optional<Long>> outcome = sut
                .getSequenceNumber(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(outcome, stageWillCompleteWithValueThat(equalTo(Optional.of(result))));
        verify(eventStorageAdapter, times(1)).getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
    }

    @Test
    public void getSequenceNumberPassesExceptionThrough() {
        Throwable exception = new IOException();
        when(eventStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.exceptional(exception));
        CompletableFuture<Optional<Long>> outcome = sut
                .getSequenceNumber(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(outcome, stageCompletedWithExceptionThat(equalTo(exception)));
        verify(eventStorageAdapter, times(1)).getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
    }

    @Test
    public void savePassesCallThrough() throws Exception {
        when(eventStorageAdapter.save(serializedEvent1)).thenReturn(CompletableFutures.TRUE);
        CompletableFuture<Boolean> outcome = sut.save(saveRequest1);
        assertThat(outcome, stageCompletedWithValueThat(equalTo(true)));
        verify(eventSerializer, times(1)).serialize(saveRequest1);
        verify(eventStorageAdapter, times(1)).save(serializedEvent1);
    }

    @Test
    public void savePassesExceptionThrough() throws Exception {
        Throwable exception = new IOException();
        when(eventSerializer.serialize(saveRequest1)).thenThrow(exception);
        CompletableFuture<Boolean> outcome = sut.save(saveRequest1);
        assertThat(outcome, stageCompletedWithExceptionThat(equalTo(exception)));
        verify(eventSerializer, times(1)).serialize(saveRequest1);
        verify(eventStorageAdapter, times(0)).save(serializedEvent1);
    }
}
