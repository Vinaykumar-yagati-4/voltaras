package com.voltaras.complaintservice.exception;

/**
 * Thrown when an authenticated caller is not allowed to perform the
 * operation (wrong role or not the complaint owner). Mapped to
 * {@code 403 ACCESS_DENIED}.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
