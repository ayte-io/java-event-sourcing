package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.misc.Pair;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListMatcher<T> extends DiagnosingMatcher<List<T>> {
    private final List<T> expectation;
    private final List<Function<T, Matcher<T>>> matcherFactories;

    @Override
    @Step
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    protected boolean matches(Object item, Description description) {
        attach(expectation);
        if (!(item instanceof List)) {
            return false;
        }
        List<?> actual = (List<?>) item;
        if (expectation.size() != actual.size()) {
            return false;
        }
        List<Pair<Integer, Description>> violations = new ArrayList<>(expectation.size());
        for (int i = 0; i < expectation.size(); i++) {
            T expected = expectation.get(i);
            T received = (T) actual.get(i);
            Description collector = new StringDescription();
            AtomicInteger counter = new AtomicInteger();
            matcherFactories
                    .stream()
                    .map(factory -> factory.apply(expected))
                    .forEach(matcher -> {
                        if (!matcher.matches(received)) {
                            collector.appendValue(counter.incrementAndGet());
                            collector.appendText(": ");
                            matcher.describeMismatch(received, collector);
                        }
                    });
            if (counter.get() > 0) {
                violations.add(new Pair<>(i, collector));
            }
        }
        if (violations.isEmpty()) {
            return true;
        }
        String violationText = violations.stream()
                .map(violation -> "element #" + violation.getLeft() + ": " + violation.getRight().toString())
                .collect(Collectors.joining("; "));
        description.appendText(violationText);
        return false;
    }

    @Attachment("expectation")
    @SuppressWarnings("UnusedReturnValue")
    private String attach(List<T> expectation) {
        return Objects.toString(expectation);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches list " + expectation + " with matchers " + matcherFactories);
    }

    @Factory
    public static <T> Matcher<List<T>> matchesList(List<T> expectation, List<Function<T, Matcher<T>>> factories) {
        return new ListMatcher<>(expectation, factories);
    }
}
