package io.ayte.es.test.matchers;

import io.qameta.allure.Attachment;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ObjectPropertyMatcher<T> extends DiagnosingMatcher<T> {
    private final T expectation;
    private final String[] properties;

    public ObjectPropertyMatcher(T expectation) {
        Objects.requireNonNull(expectation);
        this.expectation = expectation;
        List<Class> hierarchy = new LinkedList<>();
        Class cursor = expectation.getClass();
        while (cursor != null) {
            hierarchy.add(cursor);
            cursor = cursor.getSuperclass();
        }
        properties = hierarchy.stream()
                .flatMap(symbol -> Arrays.stream(symbol.getDeclaredFields()))
                .map(Field::getName)
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public boolean matches(Object item, Description description) {
        attach(expectation);
        attach(properties);
        List<Violation> violations = getViolations(item);
        if (violations.isEmpty()) {
            return true;
        }
        String violationText = getViolations(item).stream()
                .map(violation -> {
                    StringBuilder builder = new StringBuilder()
                            .append("expected property `")
                            .append(violation.getProperty())
                            .append("` to be `")
                            .append(violation.getExpectation())
                            .append("`, but ");
                    if (violation.isMissing()) {
                        builder.append("it is not present");
                    } else {
                        builder
                                .append("it value was `")
                                .append(violation.getValue())
                                .append('`');
                    }
                    return builder.toString();
                })
                .collect(Collectors.joining("; "));
        description.appendText(violationText);
        return false;
    }

    @Attachment("expectation")
    @SuppressWarnings("UnusedReturnValue")
    private String attach(T expectation) {
        return Objects.toString(expectation);
    }

    @Attachment("properties")
    @SuppressWarnings("UnusedReturnValue")
    private String attach(String[] properties) {
        return Arrays.toString(properties);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has same " + Arrays.toString(properties) + " properties as " + expectation);
    }

    private List<Violation> getViolations(Object item) {
        Class targetClass = item.getClass();
        return Arrays.stream(properties)
                .map(property -> {
                    try {
                        Field targetField = targetClass.getDeclaredField(property);
                        targetField.setAccessible(true);
                        Field expectationField = expectation.getClass().getDeclaredField(property);
                        expectationField.setAccessible(true);
                        Object targetValue = targetField.get(item);
                        Object expectationValue = expectationField.get(expectation);
                        if (Objects.equals(targetValue, expectationValue)) {
                            return null;
                        }
                        return Violation.different(property, expectationValue, targetValue);
                    } catch (NoSuchFieldException e) {
                        return Violation.missing(property);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Data
    private static class Violation {
        private final String property;
        private final boolean missing;
        private final Object expectation;
        private final Object value;

        public static Violation missing(String property) {
            return new Violation(property, true, null, null);
        }

        public static Violation different(String property, Object expectation, Object value) {
            return new Violation(property, false, expectation, value);
        }
    }

    @Factory
    public static <T> ObjectPropertyMatcher<T> hasSamePropertiesAs(T expectation) {
        return new ObjectPropertyMatcher<>(expectation);
    }

    @Factory
    public static <T> ObjectPropertyMatcher<T> hasSamePropertiesAs(T expectation, String[] properties) {
        return new ObjectPropertyMatcher<>(expectation, properties);
    }
}
