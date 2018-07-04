package io.ayte.es.api.v1;

public interface IdentifierConverter<ID> {
    String encode(ID id);
    ID decode(String encoded);
}
