package io.ayte.es.storage;

import io.ayte.es.api.internal.EntityConfiguration;
import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.test.SimpleInfrastructureTestBase;
import io.ayte.es.utility.CompletableFutures;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithExceptionThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithValueThat;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static io.ayte.es.test.matchers.BooleanMatcher.isFalse;
import static io.ayte.es.test.matchers.BooleanMatcher.isTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SnapshotRemovalFacilityTest extends SimpleInfrastructureTestBase {
    private SnapshotRemovalFacility sut;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(1)
                .snapshotCleanupProbability(1.0)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        sut = new SnapshotRemovalFacility(snapshotStorageAdapter, registry, probabilityArbiter);
    }

    @Test
    public void eligibleReturnsFalseOnNonRemovalStorage() {
        when(snapshotStorageAdapter.supportsRemoval())
                .thenReturn(false);

        assertThat(sut.eligible(snapshot1), isFalse());
    }

    public static Stream<Arguments> disabledRepairProvider() {
        return Stream.of(
                Arguments.of(-1, false),
                Arguments.of(-1, true),
                Arguments.of(0, false),
                Arguments.of(0, true),
                Arguments.of(1, false)
        );
    }

    @ParameterizedTest
    @MethodSource("disabledRepairProvider")
    public void eligibleReturnsFalseOnDisabledRepair(long length, boolean arbiterResponse) {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(length)
                .snapshotCleanupProbability(arbiterResponse ? 1.0 : 0.0)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        assertThat(sut.eligible(snapshot1), isFalse());
    }

    @Test
    public void eligibleReturnsTrueOnEnabledRepair() {
        assertThat(sut.eligible(snapshot1), isTrue());
    }

    @Test
    public void processReturnsSnapshotOnDisabledStorage() {
        when(snapshotStorageAdapter.supportsRemoval())
                .thenReturn(false);

        assertThat(sut.process(snapshot1), stageWillCompleteWithValueThat(optionalWithValue(is(snapshot1))));
        verify(snapshotStorageAdapter, times(0)).remove(any(), any(), anyLong());
    }

    @ParameterizedTest
    @MethodSource("disabledRepairProvider")
    public void processReturnsSnapshotOnDisabledRepair(long length, boolean arbiterResponse) {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(length)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(probabilityArbiter.decide(anyDouble()))
                .thenReturn(arbiterResponse);

        assertThat(sut.process(snapshot1), stageWillCompleteWithValueThat(optionalWithValue(is(snapshot1))));
        verify(snapshotStorageAdapter, times(0)).remove(any(), any(), anyLong());
    }

    public static Stream<Arguments> actualSnapshotVersionsProvider() {
        return Stream.of(
                Arguments.of(5, 5L, 5),
                Arguments.of(5, 6L, 2),
                Arguments.of(5, null, 2)
        );
    }

    public static Stream<Arguments> outdatedSnapshotVersionsProvider() {
        return Stream.of(
                Arguments.of(5, 6, 1),
                Arguments.of(5, 25, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("actualSnapshotVersionsProvider")
    public void processReturnsActualSnapshot(long length, Long currentNumber, long snapshotNumber) {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(length)
                .snapshotCleanupProbability(1.0)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(snapshotStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.optional(currentNumber));

        Snapshot<Integer, Integer> snapshot = snapshot1.toBuilder()
                .sequenceNumber(snapshotNumber)
                .build();

        assertThat(sut.process(snapshot), stageWillCompleteWithValueThat(optionalWithValue(is(snapshot))));
        verify(probabilityArbiter, times(1)).decide(anyDouble());
        verify(snapshotStorageAdapter, times(0)).remove(any(), any(), anyLong());
    }

    @Test
    public void processReturnsSnapshotOnFailedRemoval() {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(5)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(snapshotStorageAdapter.remove(any(), any(), anyLong()))
                .thenReturn(CompletableFutures.FALSE);

        when(snapshotStorageAdapter.getSequenceNumber(any(), any()))
                .thenReturn(CompletableFutures.optional(7L));

        Snapshot<Integer, Integer> snapshot = snapshot1.toBuilder()
                .sequenceNumber(1)
                .build();

        assertThat(sut.process(snapshot), stageWillCompleteWithValueThat(optionalWithValue(is(snapshot))));
        verify(snapshotStorageAdapter, times(1)).remove(any(), any(), anyLong());
    }

    @ParameterizedTest
    @MethodSource("outdatedSnapshotVersionsProvider")
    public void processReturnsEmptyOptionalOnRemoval(long historyLength, long currentNumber, long snapshotNumber) {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(historyLength)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(snapshotStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.optional(currentNumber));

        Snapshot<Integer, Integer> snapshot = snapshot1.toBuilder()
                .sequenceNumber(snapshotNumber)
                .build();

        assertThat(sut.process(snapshot), stageWillCompleteWithValueThat(emptyOptional()));
    }

    @Test
    public void processPassesExceptionIfConfigured() {
        val exception = new RuntimeException();
        when(snapshotStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.exceptional(exception));

        assertThat(sut.process(snapshot1), stageWillCompleteWithExceptionThat(is(exception)));
        verify(snapshotStorageAdapter, times(1)).getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
        verify(snapshotStorageAdapter, times(0))
                .remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), snapshot1.getSequenceNumber());
    }

    @Test
    public void processSuppressesExceptionIfConfigured() {
        val configuration = EntityConfiguration.DEFAULT.toBuilder()
                .snapshotHistoryLength(5)
                .suppressRepairErrors(true)
                .build();
        when(descriptor.getConfiguration())
                .thenReturn(configuration);

        when(snapshotStorageAdapter.getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded()))
                .thenReturn(CompletableFutures.exceptional(new RuntimeException()));

        assertThat(sut.process(snapshot1), stageWillCompleteWithValueThat(optionalWithValue(is(snapshot1))));
        verify(snapshotStorageAdapter, times(1)).getSequenceNumber(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded());
        verify(snapshotStorageAdapter, times(0))
                .remove(ENTITY_TYPE.getType(), ENTITY_ID.getEncoded(), snapshot1.getSequenceNumber());
    }
}
