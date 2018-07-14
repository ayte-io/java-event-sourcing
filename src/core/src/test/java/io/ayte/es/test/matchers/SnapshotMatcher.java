package io.ayte.es.test.matchers;

import io.ayte.es.api.v1.Snapshot;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.List;

public class SnapshotMatcher<E, ID> extends DiagnosingMatcher<Snapshot<E, ID>> {
    private final Snapshot<E, ID> expectation;
    private final ObjectPropertyMatcher<Snapshot<E, ID>> matcher;

    public SnapshotMatcher(Snapshot<E, ID> expectation) {
        this.expectation = expectation;
        this.matcher = ObjectPropertyMatcher.hasSamePropertiesAs(expectation);
    }

    @Override
    public boolean matches(Object item, Description description) {
        return matcher.matches(item, description);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("equal to " + expectation);
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
