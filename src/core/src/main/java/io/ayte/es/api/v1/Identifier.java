package io.ayte.es.api.v1;

import lombok.Data;

@Data
@SuppressWarnings("squid:S00119")
public class Identifier<ID> {
    private final ID symbol;
    private final String encoded;
}
