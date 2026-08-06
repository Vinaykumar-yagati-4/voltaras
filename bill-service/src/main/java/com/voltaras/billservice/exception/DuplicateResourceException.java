package com.voltaras.billservice.exception;

/**
 * Thrown when a bill already exists for the same user, meter number,
 * billing month and billing year.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
