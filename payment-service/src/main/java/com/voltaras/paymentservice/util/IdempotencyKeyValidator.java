package com.voltaras.paymentservice.util;

import com.voltaras.paymentservice.exception.BadRequestException;

/**
 * Validates the client-supplied {@code Idempotency-Key} header used by both
 * recharge orders and bill payments.
 */
public final class IdempotencyKeyValidator {

    public static final int MAX_LENGTH = 100;

    private IdempotencyKeyValidator() {
    }

    /**
     * Requires a non-blank key no longer than {@value #MAX_LENGTH}
     * characters.
     *
     * @param idempotencyKey value of the Idempotency-Key header
     */
    public static void requireValid(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException(
                    "Idempotency-Key header is required");
        }

        if (idempotencyKey.length() > MAX_LENGTH) {
            throw new BadRequestException(
                    "Idempotency-Key header must not exceed "
                            + MAX_LENGTH + " characters");
        }
    }
}
