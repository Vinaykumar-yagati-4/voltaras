package com.voltaras.paymentservice.exception;

/**
 * Thrown when a request itself is invalid (for example a missing
 * authenticated user or a blank idempotency key).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
