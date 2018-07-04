package io.ayte.es.api.v1;

/**
 * Entity factory may be used to inject dependencies into newly created entities
 * before their first {@link Mutation} application or to populate entity
 * restored from snapshot with any necessary dependencies.
 *
 * @param <E> Entity type
 */
public interface EntityFactory<E> {
    /**
     * This method would be called when it comes to application of event with
     * number 1 (effectively the start of entity history). Please note that
     * event #1 may be applied <i>any</i> amount of times - for any read as long
     * as entity doesn't have snapshots, so putting business logic here seems to
     * be bad idea.
     */
    E create();

    /**
     * This method is called when entity is restored from snapshot. Since
     * snapshot restoration cancels event #1 application, it requires another
     * functionality to provide dependencies. Following code will use return
     * value, so implementation may freely create and return completely new
     * entity instance.
     */
    E populate(E entity);
}
