package com.voltaras.paymentservice.exception;

/**
 * Thrown when the user identity cannot be verified with the Auth
 * Service: the Auth Service rejected the forwarded token, the returned
 * user ID does not match X-User-Id, the returned role does not match
 * X-User-Role, or the profile is incomplete. Mapped to
 * {@code 401 UNAUTHORIZED_USER}.
 */
public class UnauthorizedUserException extends RuntimeException {

    public UnauthorizedUserException(String message) {
        super(message);
    }
}
