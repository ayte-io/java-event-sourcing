package io.ayte.es.api.v1.exception;

public class MissingVersionException extends RuntimeException {
    public MissingVersionException(String message) {
        super(message);
    }
}
