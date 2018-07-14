package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.storage.SerializedSnapshot;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

import java.util.Objects;

public class SerializedSnapshotMatcher extends DiagnosingMatcher<SerializedSnapshot> {
    private final SerializedSnapshot expectation;
    private final ObjectPropertyMatcher<SerializedSnapshot> matcher;

    public SerializedSnapshotMatcher(SerializedSnapshot expectation) {
        this.expectation = expectation;
        this.matcher = ObjectPropertyMatcher.hasSamePropertiesAs(expectation);
    }

    @Step
    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean matches(Object item, Description description) {
        attach(expectation);
        return matcher.matches(item, description);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("equal to " + expectation);
    }

    @Attachment("expectation")
    @SuppressWarnings("UnusedReturnValue")
    private String attach(SerializedSnapshot expectation) {
        return Objects.toString(expectation);
    }

    @Factory
    public static Matcher<SerializedSnapshot> equalTo(SerializedSnapshot expectation) {
        return new SerializedSnapshotMatcher(expectation);
    }

    @Factory
    public static Matcher<SerializedSnapshot> equalToSnapshot(SerializedSnapshot expectation) {
        return equalTo(expectation);
    }

    @Factory
    public static Matcher<SerializedSnapshot> equalToSerializedSnapshot(SerializedSnapshot expectation) {
        return equalTo(expectation);
    }
}
