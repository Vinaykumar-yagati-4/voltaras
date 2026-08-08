package com.voltaras.paymentservice.exception;

/**
 * Thrown when the Auth Service reports that the authenticated user does
 * not exist (HTTP 404 from the internal user endpoint). Mapped to
 * {@code 404 USER_NOT_FOUND}.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found with id: '" + userId + "'");
    }
}
