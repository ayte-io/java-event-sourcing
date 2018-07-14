package io.ayte.es.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProbabilityArbiterTest {

    private Random random;
    private ProbabilityArbiter sut;

    @BeforeEach
    public void setUp() {
        random = mock(Random.class);
        sut = new ProbabilityArbiter(random);
    }

    @Test
    public void returnsTrueForProbabilityHigherThanRandom() {
        when(random.nextDouble()).thenReturn(0.2);
        assertTrue(sut.decide(0.5));
    }

    @Test
    public void returnsTrueForProbabilityEqualToRandom() {
        when(random.nextDouble()).thenReturn(0.5);
        assertTrue(sut.decide(0.5));
    }

    @Test
    public void returnsFalseForProbabilityLessThanRandom() {
        when(random.nextDouble()).thenReturn(0.5);
        assertFalse(sut.decide(0.2));
    }
}
