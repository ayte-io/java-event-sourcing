package io.ayte.es.storage;

import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.storage.SerializedEvent;
import io.ayte.es.test.SimpleInfrastructureTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.ayte.es.test.matchers.EventMatcher.equalToEvents;
import static io.ayte.es.test.matchers.SerializedEventMatcher.equalToSerializedEvent;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventSerializerTest extends SimpleInfrastructureTestBase {
    private EventSerializer sut;

    @BeforeEach
    @SuppressWarnings({"unchecked"})
    public void setUp() throws Exception {
        super.setUp();
        sut = new EventSerializer(registry, serializer, timeSource);
    }

    @Test
    public void correctlyDeserializesEvents() throws Exception {
        List<SerializedEvent> events = Arrays.asList(serializedEvent1, serializedEvent2);
        assertThat(sut.deserializeAll(events), equalToEvents(Arrays.asList(event1, event2)));
        verify(serializer, times(2)).deserialize(any(), any());
    }

    @Test
    public void correctlySerializesEvents() throws Exception {
        assertThat(sut.serialize(event1), equalToSerializedEvent(serializedEvent1));
        assertThat(sut.serialize(event2), equalToSerializedEvent(serializedEvent2));
        verify(serializer, times(2)).serialize(any());
    }

    @Test
    public void correctlySerializesRequests() throws Exception {
        assertThat(sut.serialize(saveRequest1), equalToSerializedEvent(serializedEvent1));
        assertThat(sut.serialize(saveRequest2), equalToSerializedEvent(serializedEvent2));
        verify(serializer, times(2)).serialize(any());
    }

    @Test
    public void setsMetadataDuringSerialization() throws Exception {
        Event<Integer, Integer> event = event1.toBuilder().metadata(null).build();
        assertThat(sut.serialize(event).getMetadata(), not(nullValue()));
        verify(serializer, times(1)).serialize(any());
    }

    @Test
    public void setsMetadataDuringDeserialization() throws Exception {
        SerializedEvent event = serializedEvent1.toBuilder().metadata(null).build();
        assertThat(sut.deserialize(event).getMetadata(), not(nullValue()));
        verify(serializer, times(1)).deserialize(any(), any());
    }

    @Test
    public void passesThroughSerializationError() throws Exception {
        when(serializer.serialize(any())).thenThrow(new IOException());
        // todo use hamcrest?
        assertThrows(IOException.class, () -> sut.serialize(event1));
        verify(serializer, times(1)).serialize(any());
    }

    @Test
    public void passesThroughDeserializationError() throws Exception {
        when(serializer.deserialize(any(), any())).thenThrow(new IOException());
        // todo use hamcrest?
        assertThrows(IOException.class, () -> sut.deserialize(serializedEvent1));
        verify(serializer, times(1)).deserialize(any(), any());
    }
}
