package com.voltaras.complaintservice.exception;

/**
 * Thrown when a requested resource does not exist (or is not visible to
 * the caller). Mapped to {@code 404 RESOURCE_NOT_FOUND}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(resource + " not found with " + field + ": " + value);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
