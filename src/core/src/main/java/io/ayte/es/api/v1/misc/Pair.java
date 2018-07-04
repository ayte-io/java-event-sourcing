package io.ayte.es.api.v1.misc;

import lombok.Data;

@Data
public class Pair<L, R> {
    private final L left;
    private final R right;

    public <T> Pair<T, R> withLeft(T left) {
        return new Pair<>(left, right);
    }

    public <T> Pair<L, T> withRight(T right) {
        return new Pair<>(left, right);
    }
}
