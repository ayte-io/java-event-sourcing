package io.ayte.es.storage;

import io.ayte.es.api.internal.EntityConfiguration;
import io.ayte.es.api.v1.DependencyInjector;
import io.ayte.es.api.v1.DeprecatedMutation;
import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.Mutation;
import io.ayte.es.api.v1.MutationType;
import io.ayte.es.api.v1.exception.IllegalMutationUpgradeException;
import io.ayte.es.test.SimpleInfrastructureTestBase;
import io.ayte.es.utility.CompletableFutures;
import io.ayte.es.utility.ProbabilityArbiter;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageCompletedWithExceptionThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageCompletedWithValueThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithExceptionThat;
import static com.spotify.hamcrest.future.CompletableFutureMatchers.stageWillCompleteWithValueThat;
import static io.ayte.es.test.matchers.BooleanMatcher.isFalse;
import static io.ayte.es.test.matchers.BooleanMatcher.isTrue;
import static io.ayte.es.test.matchers.EventMatcher.equalToEvent;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventRepairFacilityTest extends SimpleInfrastructureTestBase {
    private EventRepairFacility sut;
    private ProbabilityArbiter probabilityArbiter;
    private DependencyInjector injector;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        probabilityArbiter = mock(ProbabilityArbiter.class);
        when(probabilityArbiter.decide(anyDouble())).thenReturn(true);

        injector = mock(DependencyInjector.class);

        sut = new EventRepairFacility(registry, eventStorageAdapter, eventSerializer, probabilityArbiter, injector);
    }

    @Test
    public void doesNotRunForNonDeprecatedMutation() {
        assertThat(sut.process(event1), stageCompletedWithValueThat(is(event1)));
        verify(eventStorageAdapter, times(0)).replace(any(), any());
    }

    @Test
    public void processRunsForDeprecatedMutation() {
        assertThat(sut.process(deprecatedEvent1), stageCompletedWithValueThat(equalToEvent(event1)));
        verify(eventStorageAdapter, times(1)).replace(any(), any());
    }

    @Test
    public void processDoesNotRunIfProbabilityCheckerRejects() {
        when(probabilityArbiter.decide(anyDouble()))
                .thenReturn(false);

        assertThat(sut.process(deprecatedEvent1), stageWillCompleteWithValueThat(is(deprecatedEvent1)));
        verify(eventStorageAdapter, times(0)).replace(any(), any());
    }

    @Test
    public void processDoesNotRunForNonReplaceableStorage() {
        when(eventStorageAdapter.supportsReplace())
                .thenReturn(false);

        assertThat(sut.process(deprecatedEvent1), stageWillCompleteWithValueThat(is(deprecatedEvent1)));
        verify(eventStorageAdapter, times(0)).replace(any(), any());
    }

    @Test
    public void processSuppressesErrorIfConfiguredSo() {
        when(descriptor.getConfiguration())
                .thenReturn(EntityConfiguration.builder().suppressRepairErrors(true).build());

        when(eventStorageAdapter.replace(any(), any()))
                .thenReturn(CompletableFutures.exceptional(new RuntimeException()));

        assertThat(sut.process(deprecatedEvent1), stageWillCompleteWithValueThat(is(deprecatedEvent1)));
        verify(eventStorageAdapter, times(1)).replace(any(), any());
    }

    @Test
    public void processDoesNotSuppressErrorIfConfiguredSo() {
        when(descriptor.getConfiguration())
                .thenReturn(EntityConfiguration.builder().suppressRepairErrors(false).build());

        RuntimeException throwable = new RuntimeException();
        when(eventStorageAdapter.replace(any(), any()))
                .thenReturn(CompletableFutures.exceptional(throwable));

        assertThat(sut.process(deprecatedEvent1), stageWillCompleteWithExceptionThat(is(throwable)));
    }

    @Test
    public void processGracefullyHandlesCasFailure() {
        when(eventStorageAdapter.replace(any(), any()))
                .thenReturn(CompletableFutures.FALSE);

        assertThat(sut.process(deprecatedEvent1), stageWillCompleteWithValueThat(is(deprecatedEvent1)));
        verify(eventStorageAdapter, times(1)).replace(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processThrowsOnSameMutationType() {
        AtomicReference<DeprecatedMutation<Integer, Integer>> container = new AtomicReference<>();
        DeprecatedMutation<Integer, Integer> mutation = new DeprecatedMutation<Integer, Integer>() {
            @Override
            public CompletableFuture<Mutation<Integer, Integer>> upgrade(DependencyInjector injector) {
                return CompletableFutures.completed(container.get());
            }

            @Override
            public Integer apply(Integer entity, Context<Integer, Integer> context) {
                return entity;
            }
        };
        container.set(mutation);

        MutationType<?, Integer, Integer> type = MutationType.builder()
                .symbol((Class) mutation.getClass())
                .type("identity")
                .version(1)
                .build();

        when(descriptor.getMutationType((Class) mutation.getClass()))
                .thenReturn(type);

        Event<Integer, Integer> event = makeEvent(mutation, type, 1);
        Matcher<Throwable> matcher = instanceOf(IllegalMutationUpgradeException.class);
        assertThat(sut.process(event), stageCompletedWithExceptionThat(matcher));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processDoesNotThrowOnTypeChange() {
        DeprecatedMutation<Integer, Integer> mutation = new DeprecatedMutation<Integer, Integer>() {
            @Override
            public CompletableFuture<Mutation<Integer, Integer>> upgrade(DependencyInjector injector) {
                return CompletableFutures.completed(mutationA);
            }

            @Override
            public Integer apply(Integer entity, Context<Integer, Integer> context) {
                return entity;
            }
        };
        MutationType<?, Integer, Integer> type = MutationType.builder()
                .symbol((Class) mutation.getClass())
                .type("identity")
                .version(1)
                .build();
        when(descriptor.getMutationType((Class) mutation.getClass())).thenReturn(type);
        Event<Integer, Integer> event = makeEvent(mutation, type, 1);
        assertThat(sut.process(event), stageWillCompleteWithValueThat(equalToEvent(event1)));
        verify(eventStorageAdapter, times(1)).replace(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processDoesNotThrowOnVersionDowngrade() {
        DeprecatedMutation<Integer, Integer> mutation = new DeprecatedMutation<Integer, Integer>() {
            @Override
            public CompletableFuture<Mutation<Integer, Integer>> upgrade(DependencyInjector injector) {
                return CompletableFutures.completed(mutationA);
            }

            @Override
            public Integer apply(Integer entity, Context<Integer, Integer> context) {
                return entity;
            }
        };
        MutationType<?, Integer, Integer> type = MutationType.builder()
                .symbol((Class) mutation.getClass())
                .type("increment")
                .version(3)
                .build();

        when(descriptor.getMutationType((Class) mutation.getClass()))
                .thenReturn(type);

        Event<Integer, Integer> event = makeEvent(mutation, type, 1);
        assertThat(sut.process(event), stageWillCompleteWithValueThat(equalToEvent(event1)));
        verify(eventStorageAdapter, times(1)).replace(any(), any());
    }

    @Test
    public void eligibleReturnsFalseIfReplaceIsNotSupported() {
        when(eventStorageAdapter.supportsReplace())
                .thenReturn(false);

        assertThat(sut.eligible(deprecatedEvent1), isFalse());
    }

    @Test
    public void eligibleReturnsFalseForRegularEvent() {
        assertThat(sut.eligible(event1), isFalse());
    }

    @Test
    public void eligibleReturnsTrueForDeprecatedEvent() {
        assertThat(sut.eligible(deprecatedEvent1), isTrue());
    }
}
