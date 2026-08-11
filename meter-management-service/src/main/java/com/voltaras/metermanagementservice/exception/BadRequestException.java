package com.voltaras.metermanagementservice.exception;

/**
 * Thrown when a request is valid at the bean-validation level but
 * violates a business rule, e.g. assigning a meter that is already
 * assigned to another consumer.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
