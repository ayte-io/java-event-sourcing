package io.ayte.es.api.v1;

/**
 * Basic interface for providing
 */
public interface DependencyInjector {
    <T> T get(Class<T> type);
}
