package io.ayte.es.api.internal;

import lombok.Data;

@Data
@SuppressWarnings({"squid:S00119"})
public class ObjectIdentifier<E, ID> {
    private final Class<E> entity;
    private final ID id;
    private final long sequenceNumber;
}
