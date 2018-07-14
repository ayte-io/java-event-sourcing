package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.Snapshot;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SnapshotMatcher<E, ID> extends DiagnosingMatcher<Snapshot<E, ID>> {
    private final Snapshot<E, ID> expectation;
    private final ObjectPropertyMatcher<Snapshot<E, ID>> matcher;

    public SnapshotMatcher(Snapshot<E, ID> expectation) {
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
    private String attach(Snapshot<E, ID> expectation) {
        return Objects.toString(expectation);
    }

    @Factory
    public static <E, ID> Matcher<Snapshot<E, ID>> equalTo(Snapshot<E, ID> expectation) {
        return new SnapshotMatcher<>(expectation);
    }

    @Factory
    public static <E, ID> Matcher<Snapshot<E, ID>> equalToSnapshot(Snapshot<E, ID> expectation) {
        return new SnapshotMatcher<>(expectation);
    }

    @Factory
    public static <E, ID> Matcher<List<Snapshot<E, ID>>> equalToSnapshots(List<Snapshot<E, ID>> expectation) {
        return ListMatcher.matchesList(expectation, Collections.singletonList(SnapshotMatcher::equalTo));
    }
}
