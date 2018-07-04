package io.ayte.es.api.v1;

import lombok.Data;

@Data
@SuppressWarnings("squid:S00119")
public class MutationType<M extends Mutation<E, ID>, E, ID> {
    private final M symbol;
    private final String type;
    private final int version;
}
