package io.ayte.es.api.v1.exception;

public class IllegalMutationUpgradeException extends RuntimeException {
    public IllegalMutationUpgradeException() {
    }

    public IllegalMutationUpgradeException(String message) {
        super(message);
    }

    public IllegalMutationUpgradeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalMutationUpgradeException(Throwable cause) {
        super(cause);
    }

    public IllegalMutationUpgradeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
