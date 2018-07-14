package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.SaveRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class SaveRequestMatcher<E, ID> extends BaseMatcher<SaveRequest<E, ID>> {
    private final SaveRequest<E, ID> expectation;
    private final ObjectPropertyMatcher<SaveRequest<E, ID>> matcher;

    public SaveRequestMatcher(SaveRequest<E, ID> expectation) {
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
}
