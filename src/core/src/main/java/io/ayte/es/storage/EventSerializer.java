package io.ayte.es.storage;


import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.v1.Event;
import io.ayte.es.api.v1.Identifier;
import io.ayte.es.api.v1.SaveRequest;
import io.ayte.es.api.v1.Serializer;
import io.ayte.es.api.v1.TimeSource;
import io.ayte.es.api.v1.storage.SerializedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"squid:S00119"})
public class EventSerializer {
    private final DescriptorRegistry registry;
    private final Serializer serializer;
    private final TimeSource timeSource;

    @SuppressWarnings({"unchecked"})
    public <E, ID> SerializedEvent serialize(SaveRequest<E, ID> request) throws IOException {
        val descriptor = registry.<E, ID>getDescriptor(request.getEntity());
        val mutationType = descriptor.getMutationType(request.getMutation().getClass());
        val identifier = new Identifier<>(
                request.getId(),
                descriptor.getIdentifierConverter().encode(request.getId())
        );
        val event = Event.<E, ID>builder()
                .entityType(descriptor.getType())
                .entityId(identifier)
                .mutationType(mutationType)
                .mutation(request.getMutation())
                .sequenceNumber(request.getSequenceNumber())
                .occurredAt(request.getOccurredAt())
                .acknowledgedAt(timeSource.getCurrentTime())
                .metadata(request.getMetadata())
                .build();
        return serialize(event);
    }

    public <E, ID> SerializedEvent serialize(Event<E, ID> event) throws IOException {
        try {
            return SerializedEvent.builder()
                    .entityType(event.getEntityType().getType())
                    .entityId(event.getEntityId().getEncoded())
                    .sequenceNumber(event.getSequenceNumber())
                    .mutationType(event.getMutationType().getType())
                    .mutationVersion(event.getMutationType().getVersion())
                    .payload(serializer.serialize(event.getMutation()))
                    .occurredAt(event.getOccurredAt())
                    .acknowledgedAt(event.getAcknowledgedAt())
                    .metadata(event.getMetadata() == null ? Collections.emptyMap() : event.getMetadata())
                    .build();
        } catch (IOException | RuntimeException e) {
            log.error("Error during conversion {} to serialized event: {}", event, e);
            throw e;
        }
    }

    public <E, ID> List<Event<E, ID>> deserializeAll(Collection<SerializedEvent> events) throws IOException {
        val target = new ArrayList<Event<E, ID>>(events.size());
        for (val event : events) {
            target.add(deserialize(event));
        }
        return target;
    }

    public <E, ID> Event<E, ID> deserialize(SerializedEvent event) throws IOException {
        val descriptor = registry.<E, ID>getDescriptor(event.getEntityType());
        val id = descriptor.getIdentifierConverter().decode(event.getEntityId());
        val mutationType = descriptor.getMutationType(event.getMutationType(), event.getMutationVersion());
        try {
            return Event.<E, ID>builder()
                    .entityType(descriptor.getType())
                    .entityId(new Identifier<>(id, event.getEntityId()))
                    .mutationType(mutationType)
                    .mutation(serializer.deserialize(event.getPayload(), mutationType.getSymbol()))
                    .sequenceNumber(event.getSequenceNumber())
                    .occurredAt(event.getOccurredAt())
                    .acknowledgedAt(event.getAcknowledgedAt())
                    .metadata(event.getMetadata() == null ? Collections.emptyMap() : event.getMetadata())
                    .build();
        } catch (IOException | RuntimeException e) {
            log.error("Exception thrown during {} deserialization: {}", event, e);
            throw e;
        }
    }
}
