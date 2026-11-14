package com.phinixlabs.livestandings.common.exception;

/** Request / path / query validation failure → HTTP 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
