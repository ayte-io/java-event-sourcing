package io.ayte.es.storage;

import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.api.v1.storage.SerializedSnapshot;
import io.ayte.es.test.SimpleInfrastructureTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.ayte.es.test.matchers.ExecutionMatcher.willThrow;
import static io.ayte.es.test.matchers.SerializedSnapshotMatcher.equalToSerializedSnapshot;
import static io.ayte.es.test.matchers.SnapshotMatcher.equalToSnapshot;
import static io.ayte.es.test.matchers.SnapshotMatcher.equalToSnapshots;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SnapshotSerializerTest extends SimpleInfrastructureTestBase {

    private SnapshotSerializer sut;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        sut = new SnapshotSerializer(registry, serializer);
    }

    @Test
    public void serializeCorrectlyProcessesInput() throws Exception {
        assertThat(sut.serialize(snapshot1), equalToSerializedSnapshot(serializedSnapshot1));
        assertThat(sut.serialize(snapshot2), equalToSerializedSnapshot(serializedSnapshot2));
        verify(serializer, times(2)).serialize(any());
    }

    @Test
    public void serializePassesThroughException() throws Exception {
        IOException exception = new IOException();
        when(serializer.serialize(any()))
                .thenThrow(exception);

        assertThat(() -> sut.serialize(snapshot1), willThrow(exception));
        verify(serializer, times(1)).serialize(any());
    }

    @Test
    public void serializeFillsEmptyMetadata() throws Exception {
        Snapshot<Integer, Integer> snapshot = snapshot1.toBuilder().metadata(null).build();
        assertThat(sut.serialize(snapshot).getMetadata(), not(nullValue()));
        verify(serializer, times(1)).serialize(any());
    }

    @Test
    public void deserializeCorrectlyProcessesInput() throws Exception {
        assertThat(sut.deserialize(serializedSnapshot1), equalToSnapshot(snapshot1));
        assertThat(sut.deserialize(serializedSnapshot2), equalToSnapshot(snapshot2));
        verify(serializer, times(2)).deserialize(any(), eq(Integer.class));
    }

    @Test
    public void deserializePassesThroughException() throws Exception {
        IOException exception = new IOException();
        when(serializer.deserialize(any(), any()))
                .thenThrow(exception);

        assertThat(() -> sut.deserialize(serializedSnapshot1), willThrow(exception));
        verify(serializer, times(1)).deserialize(any(), eq(Integer.class));
    }

    @Test
    public void deserializeFillsEmptyMetadata() throws Exception {
        SerializedSnapshot serializedSnapshot = serializedSnapshot1.toBuilder().metadata(null).build();
        assertThat(sut.deserialize(serializedSnapshot).getMetadata(), not(nullValue()));
        verify(serializer, times(1)).deserialize(any(), eq(Integer.class));
    }

    @Test
    public void deserializeAllWorksCorrectly() throws Exception {
        List<SerializedSnapshot> input = Arrays.asList(serializedSnapshot1, serializedSnapshot2);
        List<Snapshot<Integer, Integer>> expectation = Arrays.asList(snapshot1, snapshot2);
        assertThat(sut.deserializeAll(input), equalToSnapshots(expectation));
    }
}
