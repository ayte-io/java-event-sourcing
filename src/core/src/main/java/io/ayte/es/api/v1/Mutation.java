package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;

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
    E apply(E entity, Context<E, ID> context);

    @Data
    @Builder(toBuilder = true)
    class Context<E, ID> {
        private final EntityType<E> type;
        private final Identifier<ID> id;
        private final long sequenceNumber;
        private final ZonedDateTime acknowledgedAt;
        private final ZonedDateTime occurredAt;
    }
}
