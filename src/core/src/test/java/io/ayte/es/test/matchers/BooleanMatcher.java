package io.ayte.es.test.matchers;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

public class BooleanMatcher {
    public static Matcher<Boolean> isTrue() {
        return CoreMatchers.is(true);
    }

    public static Matcher<Boolean> isFalse() {
        return CoreMatchers.is(false);
    }
}
