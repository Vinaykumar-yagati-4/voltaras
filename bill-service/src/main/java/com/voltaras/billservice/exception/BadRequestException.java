package com.voltaras.billservice.exception;

/**
 * Thrown when a request itself is invalid (for example a missing
 * authenticated user).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
