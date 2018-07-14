package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"entityType", "entityId", "sequenceNumber", "mutationType"})
@ToString(of = {"entityType", "entityId", "sequenceNumber", "mutationType"})
@SuppressWarnings("squid:S00119")
public class Event<E, ID> {
    private final EntityType<E> entityType;
    private final Identifier<ID> entityId;
    private final long sequenceNumber;
    private final MutationType<? extends Mutation<E, ID>, E, ID> mutationType;
    private final Mutation<E, ID> mutation;
    private final ZonedDateTime occurredAt;
    private final ZonedDateTime acknowledgedAt;
    private final Map<String, Object> metadata;

    public boolean equalIdentifier(Event<E, ID> other) {
        if (entityType == null || entityId == null) {
            return false;
        }
        return entityType.equals(other.getEntityType())
                && entityId.equals(other.getEntityId())
                && sequenceNumber == other.getSequenceNumber();
    }
}
