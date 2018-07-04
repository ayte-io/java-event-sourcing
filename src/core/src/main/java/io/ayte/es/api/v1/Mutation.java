package io.ayte.es.api.v1;

import java.time.ZonedDateTime;

/**
 * Mutation represents single unit of changes enclosed in an event.
 *
 * Mutations has to be serializable by {@link Serializer}.
 *
 * @param <E>
 * @param <ID>
 */
@SuppressWarnings("squid:S00119")
public interface Mutation<E, ID> {
    E apply(Entity<E, ID> entity, Context context);

    interface Context {
        long getEventNumber();
        ZonedDateTime getEventAcknowledgedAt();
        ZonedDateTime getEventOccurredAt();
    }
}
