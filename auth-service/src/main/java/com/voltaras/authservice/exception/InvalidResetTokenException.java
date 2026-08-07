package com.voltaras.authservice.exception;

/**
 * Thrown when a password-reset token is missing, invalid, expired,
 * already used or revoked. The message stays generic on purpose so
 * no token or account details are revealed.
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException(String message) {
        super(message);
    }
}
