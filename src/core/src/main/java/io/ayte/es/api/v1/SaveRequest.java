package io.ayte.es.api.v1;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@SuppressWarnings("squid:S00119")
@ToString(of = {"entity", "id", "sequenceNumber", "mutation"})
public class SaveRequest<E, ID> {
    private final Class<E> entity;
    private final ID id;
    private final Mutation<E, ID> mutation;
    private final ZonedDateTime occurredAt;
    private final Map<String, Object> metadata;
    private final long sequenceNumber;

    public static <E, ID> SaveRequest<E, ID> from(AppendRequest<E, ID> request, long sequenceNumber) {
        return SaveRequest.<E, ID>builder()
                .entity(request.getEntity())
                .id(request.getId())
                .mutation(request.getMutation())
                .occurredAt(request.getOccurredAt())
                .metadata(request.getMetadata())
                .sequenceNumber(sequenceNumber)
                .build();
    }
}
