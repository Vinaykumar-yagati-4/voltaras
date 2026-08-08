package com.voltaras.paymentservice.exception;

/**
 * Thrown when an idempotency key is reused with a different request
 * payload. Mapped to HTTP 409 with the IDEMPOTENCY_CONFLICT error code.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super(String.format(
                "Idempotency key '%s' was already used with a different request",
                idempotencyKey));
    }
}
