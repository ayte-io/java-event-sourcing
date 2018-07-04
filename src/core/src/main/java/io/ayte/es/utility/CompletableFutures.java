package io.ayte.es.utility;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

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

    public static <T> CompletableFuture<T> exceptional(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<Optional<T>> emptyOptional() {
        return (CompletableFuture<Optional<T>>) EMPTY_OPTIONAL;
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

    interface Task {
        void execute() throws Exception;
    }
}
