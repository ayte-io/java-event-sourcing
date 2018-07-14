package io.ayte.es.utility;

import lombok.AllArgsConstructor;

import java.util.Random;

@AllArgsConstructor
public class ProbabilityArbiter {
    private final Random random;

    public boolean decide(double probability) {
        return random.nextDouble() <= probability;
    }
}
