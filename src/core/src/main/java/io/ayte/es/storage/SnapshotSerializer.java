package io.ayte.es.storage;

import io.ayte.es.api.internal.DescriptorRegistry;
import io.ayte.es.api.v1.Identifier;
import io.ayte.es.api.v1.Serializer;
import io.ayte.es.api.v1.Snapshot;
import io.ayte.es.api.v1.storage.SerializedSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"squid:S00119"})
public class SnapshotSerializer {
    private final DescriptorRegistry registry;
    private final Serializer serializer;

    public <E, ID> SerializedSnapshot serialize(Snapshot<E, ID> snapshot) throws IOException {
        try {
            val metadata = snapshot.getMetadata();
            return SerializedSnapshot.builder()
                    .entityType(snapshot.getEntityType().getType())
                    .entityId(snapshot.getEntityId().getEncoded())
                    .sequenceNumber(snapshot.getSequenceNumber())
                    .entityVersion(snapshot.getEntityVersion())
                    .payload(serializer.serialize(snapshot.getEntity()))
                    .eventOccurredAt(snapshot.getEventOccurredAt())
                    .eventAcknowledgedAt(snapshot.getEventAcknowledgedAt())
                    .entityCreatedAt(snapshot.getEntityCreatedAt())
                    .createdAt(snapshot.getCreatedAt())
                    .metadata(metadata != null ? metadata : Collections.emptyMap())
                    .build();
        } catch (IOException | RuntimeException e) {
            log.error("Error during {} serialization: {}", snapshot, e);
            throw e;
        }
    }

    public <E, ID> Snapshot<E, ID> deserialize(SerializedSnapshot snapshot) throws IOException {
        val descriptor = registry.<E, ID>getDescriptor(snapshot.getEntityType());
        val id = descriptor.getIdentifierConverter().decode(snapshot.getEntityId());
        val metadata = snapshot.getMetadata();
        try {
            return Snapshot.<E, ID>builder()
                    .entityType(descriptor.getType())
                    .entityId(new Identifier<>(id ,snapshot.getEntityId()))
                    .sequenceNumber(snapshot.getSequenceNumber())
                    .entityVersion(snapshot.getEntityVersion())
                    .entity(serializer.deserialize(snapshot.getPayload(), descriptor.getType().getSymbol()))
                    .eventOccurredAt(snapshot.getEventOccurredAt())
                    .eventAcknowledgedAt(snapshot.getEventAcknowledgedAt())
                    .entityCreatedAt(snapshot.getEntityCreatedAt())
                    .createdAt(snapshot.getCreatedAt())
                    .metadata(metadata != null ? metadata : Collections.emptyMap())
                    .build();
        } catch (IOException | RuntimeException e) {
            log.error("Error during {} deserialization: {}", snapshot, e);
            throw e;
        }
    }

    public <E, ID>List<Snapshot<E, ID>> deserializeAll(List<SerializedSnapshot> snapshots) throws IOException {
        val target = new ArrayList<Snapshot<E, ID>>(snapshots.size());
        for (val snapshot : snapshots) {
            target.add(deserialize(snapshot));
        }
        return target;
    }
}
