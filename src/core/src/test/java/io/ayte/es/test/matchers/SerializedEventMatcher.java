package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.storage.SerializedEvent;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.List;

public class SerializedEventMatcher extends BaseMatcher<SerializedEvent> {
    private final SerializedEvent expectation;
    private final ObjectPropertyMatcher<SerializedEvent> matcher;

    public SerializedEventMatcher(SerializedEvent expectation) {
        this.expectation = expectation;
        this.matcher = ObjectPropertyMatcher.hasSamePropertiesAs(expectation);
    }

    @Override
    public boolean matches(Object item) {
        return matcher.matches(item);
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        matcher.describeMismatch(item, description);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is equal to " + expectation);
    }

    @Factory
    public static Matcher<SerializedEvent> equalTo(SerializedEvent expectation) {
        return new SerializedEventMatcher(expectation);
    }

    @Factory
    public static Matcher<SerializedEvent> equalToEvent(SerializedEvent expectation) {
        return equalTo(expectation);
    }

    @Factory
    public static Matcher<SerializedEvent> equalToSerializedEvent(SerializedEvent expectation) {
        return equalTo(expectation);
    }

    @Factory
    public static Matcher<List<SerializedEvent>> equalToEvents(List<SerializedEvent> events) {
        return ListMatcher.matchesList(events, Collections.singletonList(SerializedEventMatcher::equalTo));
    }

    @Factory
    public static Matcher<List<SerializedEvent>> equalToSerializedEvents(List<SerializedEvent> events) {
        return ListMatcher.matchesList(events, Collections.singletonList(SerializedEventMatcher::equalTo));
    }
}
