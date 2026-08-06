package com.voltaras.billservice.exception;

/**
 * Thrown when the caller is not authorized to perform an operation, for
 * example a non-ADMIN caller invoking an admin endpoint.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
