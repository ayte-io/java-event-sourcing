package io.ayte.es.test.matchers;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@RequiredArgsConstructor
public class ExecutionMatcher extends DiagnosingMatcher<ExecutionMatcher.Execution> {
    private final static Map<Execution, Throwable> RESULTS = Collections.synchronizedMap(new WeakHashMap<>());

    private final Throwable expectation;
    private final Matcher<Throwable> matcher;

    public ExecutionMatcher(Throwable expectation) {
        this.expectation = expectation;
        this.matcher = CoreMatchers.is(expectation);
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        if (!(item instanceof Execution)) {
            mismatchDescription.appendText("is not an execution");
            return false;
        }
        val error = getResult((Execution) item);
        if (error == null) {
            mismatchDescription.appendText("did not throw");
            return false;
        }
        if (matcher.matches(error)) {
            return true;
        }
        matcher.describeMismatch(error, mismatchDescription);
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("throws " + expectation);
    }

    private static Throwable getResult(Execution execution) {
        RESULTS.computeIfAbsent(execution, key -> {
            try {
                key.execute();
                return null;
            } catch (Throwable e) {
                return e;
            }
        });
        return RESULTS.get(execution);
    }

    public interface Execution {
        void execute() throws Exception;
    }

    public static Matcher<Execution> willThrow(Throwable expectation) {
        return new ExecutionMatcher(expectation);
    }
}
