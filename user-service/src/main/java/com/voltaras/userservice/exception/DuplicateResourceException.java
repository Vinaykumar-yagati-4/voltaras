package com.voltaras.userservice.exception;

/**
 * Thrown when a resource creation violates a uniqueness constraint,
 * e.g. a profile already exists for the given authUserId.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
