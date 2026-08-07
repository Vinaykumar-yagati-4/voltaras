package com.voltaras.authservice.exception;

/**
 * Thrown when a client exceeds the configured request rate for a
 * public endpoint (e.g. forgot-password).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
