package io.ayte.es.api.v1.exception;

public class UnregisteredEntityException extends RuntimeException {
    public UnregisteredEntityException(String message) {
        super(message);
    }
}
