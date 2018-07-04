package io.ayte.es.api.v1;

import java.io.IOException;

public interface Serializer {
    <T> byte[] serialize(T input) throws IOException;
    <T> T deserialize(byte[] payload, Class<T> target) throws IOException;
}
