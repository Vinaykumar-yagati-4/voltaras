package com.voltaras.paymentservice.exception;

/**
 * Thrown when the authenticated user exists but is deactivated in the
 * Auth Service. Mapped to {@code 403 USER_INACTIVE}.
 */
public class InactiveUserException extends RuntimeException {

    public InactiveUserException(Long userId) {
        super("User account is inactive: '" + userId + "'");
    }
}
