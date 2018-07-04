package io.ayte.es.api.v1.exception;

public class UnregisteredEventException extends RuntimeException {
    public UnregisteredEventException(String message) {
        super(message);
    }
}
