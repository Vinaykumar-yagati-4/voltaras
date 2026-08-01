package com.voltaras.meterreadingservice.exception;

/**
 * Thrown when an authenticated user attempts an operation they are not
 * allowed to perform (wrong role, or editing a VERIFIED/REJECTED reading).
 * Mapped to HTTP 403 FORBIDDEN.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
