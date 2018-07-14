package io.ayte.es.utility;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompletableFutures {
    public static final CompletableFuture<Void> VOID = CompletableFuture.completedFuture(null);
    public static final CompletableFuture<Boolean> TRUE = CompletableFuture.completedFuture(true);
    public static final CompletableFuture<Boolean> FALSE = CompletableFuture.completedFuture(false);
    public static final CompletableFuture EMPTY_OPTIONAL = CompletableFuture.completedFuture(Optional.empty());

    private CompletableFutures() {
        // static access only
    }

    public static <T> CompletableFuture<T> pipe(CompletableFuture<T> source, CompletableFuture<T> target) {
        source.thenApply(target::complete).exceptionally(target::completeExceptionally);
        return target;
    }

    public static <T> CompletableFuture<T> pipedTo(CompletableFuture<T> source) {
        return pipe(source, new CompletableFuture<>());
    }

    public static <T> CompletableFuture<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }

    // todo: wrap in a completion exception
    public static <T> CompletableFuture<T> exceptional(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<Optional<T>> emptyOptional() {
        return (CompletableFuture<Optional<T>>) EMPTY_OPTIONAL;
    }

    public static <T> CompletableFuture<Optional<T>> optional(T value) {
        return completed(Optional.ofNullable(value));
    }

    public static <T> CompletableFuture<T> nullFuture() {
        return completed(null);
    }

    public static <T> CompletableFuture<T> execute(Callable<T> callable) {
        try {
            return completed(callable.call());
        } catch (Exception e) {
            return exceptional(e);
        }
    }

    public static CompletableFuture<Void> execute(Task task) {
        try {
            task.execute();
            return VOID;
        } catch (Exception e) {
            return exceptional(e);
        }
    }

    public static <T> CompletableFuture<T> onError(
            CompletableFuture<T> future,
            Function<Throwable, Throwable> processor
    ) {
        return future
                .handle((value, error) -> {
                    if (error != null) {
                        return CompletableFutures.<T>exceptional(processor.apply(error));
                    }
                    return completed(value);
                })
                .thenCompose(Function.identity());
    }

    public static <T> CompletableFuture<T> onError(CompletableFuture<T> future, Consumer<Throwable> consumer) {
        return future
                .handle((value, error) -> {
                    if (error != null) {
                        consumer.accept(error);
                        return CompletableFutures.<T>exceptional(error);
                    }
                    return completed(value);
                })
                .thenCompose(Function.identity());
    }

    public static <T> CompletableFuture<T> rescue(CompletableFuture<T> future, RescueOperation<T> processor) {
        return future
                .handle((value, error) -> {
                    if (error == null) {
                        return completed(value);
                    }
                    try {
                        return processor.apply(error);
                    } catch (Throwable e) {
                        return CompletableFutures.<T>exceptional(e);
                    }
                })
                .thenCompose(Function.identity());
    }

    public interface Task {
        @SuppressWarnings({"squid:S00112"})
        void execute() throws Exception;
    }

    public interface RescueOperation<T> {
        @SuppressWarnings({"squid:S00112"})
        CompletableFuture<T> apply(Throwable throwable) throws Throwable;
    }
}
