package io.ayte.es.storage;

import io.ayte.es.api.internal.EntityConfiguration;
import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.test.SimpleInfrastructureTestBase;
import io.ayte.es.utility.CompletableFutures;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageCompletedWithValueThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithExceptionThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithValueThat;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static io.ayte.es.test.matchers.BooleanMatcher.isFalse;
import static io.ayte.es.test.matchers.BooleanMatcher.isTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SnapshotRepositoryTest extends SimpleInfrastructureTestBase {

    private SnapshotRepository sut;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        sut = new SnapshotRepository(registry, snapshotStorageAdapter, snapshotSerializer, snapshotRemovalFacility);
    }

    @Test
    public void getSuccessfullyWorks() {
        val future = sut.get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageWillCompleteWithValueThat(optionalWithValue(is(snapshot1))));
        verify(snapshotRemovalFacility, times(1)).process(snapshot1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getDoesNotRunRemovalOnNonEligibleSnapshot() {
        when(snapshotRemovalFacility.eligible(any(Class.class))).thenReturn(false);
        when(snapshotRemovalFacility.eligible(any(Snapshot.class))).thenReturn(false);

        val future = sut.get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageWillCompleteWithValueThat(optionalWithValue(is(snapshot1))));
        verify(snapshotRemovalFacility, times(0)).process(any());
    }

    @Test
    public void getReactsAdequateToEmptyResponse() {
        when(snapshotStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.emptyOptional());

        val future = sut.get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageCompletedWithValueThat(emptyOptional()));
        verify(snapshotStorageAdapter, times(1)).get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1);
    }

    @Test
    public void getPassesThroughException() {
        val exception = new IOException();
        when(snapshotStorageAdapter.get(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.exceptional(exception));

        val future = sut.get(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageWillCompleteWithExceptionThat(is(exception)));
    }


    @Test
    public void getLastSuccessfullyWorks() {
        val future = sut.getLast(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(future, stageCompletedWithValueThat(optionalWithValue(is(snapshot2))));
        // let's make sure read remove doesn't work on that
        verify(snapshotRemovalFacility, times(0)).process(any());
        verify(snapshotRemovalFacility, times(0)).process(any(), any(), anyLong());
    }

    @Test
    public void getLastPassesThroughException() {
        val exception = new IOException();
        when(snapshotStorageAdapter.getLast(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.exceptional(exception));

        val future = sut.getLast(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(future, stageWillCompleteWithExceptionThat(is(exception)));
    }

    @Test
    public void listSuccessfullyWorks() {
        when(snapshotStorageAdapter.list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2))
                .thenReturn(CompletableFutures.completed(Arrays.asList(serializedSnapshot1, serializedSnapshot2)));

        val future = sut.list(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 0, 2);
        assertThat(future, stageCompletedWithValueThat(equalTo(Arrays.asList(snapshot1, snapshot2))));
        verify(snapshotStorageAdapter, times(1)).list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2);
    }

    @Test
    public void listPassesThroughException() {
        val exception = new IOException();
        when(snapshotStorageAdapter.list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2))
                .thenReturn(CompletableFutures.exceptional(exception));

        val future = sut.list(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 0, 2);
        assertThat(future, stageWillCompleteWithExceptionThat(is(exception)));
        verify(snapshotStorageAdapter, times(1)).list(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 0, 2);
    }

    @Test
    public void saveSuccessfullyWorks() {
        assertThat(sut.save(snapshot1), stageCompletedWithValueThat(isTrue()));
        verify(snapshotStorageAdapter, times(1)).save(serializedSnapshot1);
    }

    @Test
    public void savePassesStatusThrough() {
        when(snapshotStorageAdapter.save(serializedSnapshot1))
                .thenReturn(CompletableFutures.FALSE);

        assertThat(sut.save(snapshot1), stageCompletedWithValueThat(isFalse()));
        verify(snapshotStorageAdapter, times(0))
                .remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), snapshot1.getSequenceNumber());
    }

    @Test
    public void savePassesThroughException() {
        val exception = new IOException();
        when(snapshotStorageAdapter.save(serializedSnapshot1))
                .thenReturn(CompletableFutures.exceptional(exception));

        assertThat(sut.save(snapshot1), stageWillCompleteWithExceptionThat(is(exception)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void saveDoesNotTriggerReadRemovalForNegativeSnapshot() {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(5)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        assertThat(sut.save(snapshot1), stageCompletedWithValueThat(isTrue()));
        verify(snapshotRemovalFacility, times(0)).eligible(any(Class.class));
        verify(snapshotRemovalFacility, times(0)).eligible(any(Snapshot.class));
        verify(snapshotStorageAdapter, times(0)).remove(any(), any(), anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void saveDoesNotTriggerReadRemovalForInEligibleSnapshot() {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(-1)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        assertThat(sut.save(snapshot1), stageCompletedWithValueThat(isTrue()));
        verify(snapshotRemovalFacility, times(0)).eligible(any(Class.class));
        verify(snapshotRemovalFacility, times(0)).eligible(any(Snapshot.class));
        verify(snapshotStorageAdapter, times(0)).remove(any(), any(), anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void saveDoesNotTriggerReadRemovalForUnsupportingStorage() {
        when(snapshotStorageAdapter.supportsRemoval())
                .thenReturn(false);

        assertThat(sut.save(snapshot1), stageCompletedWithValueThat(isTrue()));
        verify(snapshotRemovalFacility, times(0)).eligible(any(Class.class));
        verify(snapshotRemovalFacility, times(0)).eligible(any(Snapshot.class));
        verify(snapshotStorageAdapter, times(0)).remove(any(), any(), anyLong());
    }

    @Test
    public void saveTriggersReadRemovalForEligibleSnapshot() {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(5)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        val snapshot = snapshot1.toBuilder()
                .sequenceNumber(6)
                .build();

        assertThat(sut.save(snapshot), stageCompletedWithValueThat(isTrue()));
        verify(snapshotStorageAdapter, times(1)).remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1);
    }

    @Test
    public void saveReactsAdequateToReadRemovalFailure() {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(5)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        val snapshot = snapshot1.toBuilder()
                .sequenceNumber(6)
                .build();

        val candidate = snapshot.getSequenceNumber() - configuration.getSnapshotHistoryLength();
        when(snapshotStorageAdapter.remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), candidate))
                .thenReturn(CompletableFutures.FALSE);

        assertThat(sut.save(snapshot), stageCompletedWithValueThat(isTrue()));
        verify(snapshotStorageAdapter, times(1)).remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1);
    }

    @Test
    public void replaceSuccessfullyWorks() {
        val future = sut.replace(snapshot1, snapshot2);
        assertThat(future, stageCompletedWithValueThat(isTrue()));
        verify(snapshotStorageAdapter, times(1)).replace(serializedSnapshot1, serializedSnapshot2);
    }

    @Test
    public void replacePassesStatusThrough() {
        when(snapshotStorageAdapter.replace(serializedSnapshot1, serializedSnapshot2))
                .thenReturn(CompletableFutures.FALSE);

        val future = sut.replace(snapshot1, snapshot2);
        assertThat(future, stageCompletedWithValueThat(isFalse()));
        verify(snapshotStorageAdapter, times(1)).replace(serializedSnapshot1, serializedSnapshot2);
    }

    @Test
    public void replacePassesExceptionThrough() {
        val exception = new IOException();
        when(snapshotStorageAdapter.replace(serializedSnapshot1, serializedSnapshot2))
                .thenReturn(CompletableFutures.exceptional(exception));

        val future = sut.replace(snapshot1, snapshot2);
        assertThat(future, stageWillCompleteWithExceptionThat(is(exception)));
        verify(snapshotStorageAdapter, times(1)).replace(serializedSnapshot1, serializedSnapshot2);
    }

    @Test
    public void removeSuccessfullyWorks() {
        val future = sut.remove(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageCompletedWithValueThat(isTrue()));
        verify(snapshotStorageAdapter, times(1)).remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1);
    }

    @Test
    public void removePassesStatusThrough() {
        when(snapshotStorageAdapter.remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.FALSE);

        val future = sut.remove(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageCompletedWithValueThat(isFalse()));
        verify(snapshotStorageAdapter, times(1)).remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1);
    }

    @Test
    public void removePassesExceptionThrough() {
        val exception = new IOException();
        when(snapshotStorageAdapter.remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1))
                .thenReturn(CompletableFutures.exceptional(exception));

        val future = sut.remove(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol(), 1);
        assertThat(future, stageWillCompleteWithExceptionThat(is(exception)));
        verify(snapshotStorageAdapter, times(1)).remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), 1);
    }

    @Test
    public void purgeSuccessfullyWorks() {
        val future = sut.purge(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(future, stageCompletedWithValueThat(emptyOptional()));
        verify(snapshotStorageAdapter, times(1)).purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
    }

    @Test
    public void purgePassesStatusThrough() {
        val value = 12L;
        when(snapshotStorageAdapter.purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.optional(value));

        val future = sut.purge(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(future, stageCompletedWithValueThat(optionalWithValue(equalTo(value))));
    }

    @Test
    public void purgePassesExceptionThrough() {
        val exception = new IOException();
        when(snapshotStorageAdapter.purge(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.exceptional(exception));

        val future = sut.purge(ENTITY_TYPE.getSymbol(), ENTITY_ID.getSymbol());
        assertThat(future, stageWillCompleteWithExceptionThat(is(exception)));
    }
}
