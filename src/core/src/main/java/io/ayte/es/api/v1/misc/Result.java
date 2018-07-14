package io.ayte.es.api.v1.misc;

import lombok.Data;

import java.util.Optional;

@Data
public class Result<T> {
    public static final Result UNSUCCESSFUL = new Result<>(false, null);

    private final boolean successful;
    private final T value;

    @SuppressWarnings("unchecked")
    public static <T> Result<T> failure() {
        return UNSUCCESSFUL;
    }

    public static <T> Result<T> successful(T value) {
        return new Result<>(true, value);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Result<T> from(Optional<T> container) {
        return container.map(Result::successful).orElseGet(Result::failure);
    }
}
