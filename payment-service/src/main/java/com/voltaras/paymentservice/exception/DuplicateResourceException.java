package com.voltaras.paymentservice.exception;

/**
 * Thrown when a resource already exists, for example a duplicate
 * payment reference or a duplicate idempotency key racing in.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
