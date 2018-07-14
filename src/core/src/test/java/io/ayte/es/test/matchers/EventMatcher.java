package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.Event;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EventMatcher<E, ID> extends BaseMatcher<Event<E, ID>> {
    private final Event<E, ID> expectation;
    private final ObjectPropertyMatcher<Event<E, ID>> matcher;

    public EventMatcher(Event<E, ID> expectation) {
        this.expectation = expectation;
        this.matcher = ObjectPropertyMatcher.hasSamePropertiesAs(expectation);
    }

    @Override
    @Step
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean matches(Object item) {
        attach(expectation);
        return matcher.matches(item);
    }

    @Attachment("expectation")
    @SuppressWarnings("UnusedReturnValue")
    private String attach(Event<E, ID> expectation) {
        return Objects.toString(expectation);
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        matcher.describeMismatch(item, description);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("equal to " + expectation);
    }

    public static <E, ID> Matcher<Event<E, ID>> equalTo(Event<E, ID> event) {
        return new EventMatcher<>(event);
    }

    public static <E, ID> Matcher<Event<E, ID>> equalToEvent(Event<E, ID> event) {
        return equalTo(event);
    }

    public static <E, ID> Matcher<List<Event<E, ID>>> equalToEvents(List<Event<E, ID>> events) {
        return ListMatcher.matchesList(events, Collections.singletonList(EventMatcher::equalTo));
    }
}
