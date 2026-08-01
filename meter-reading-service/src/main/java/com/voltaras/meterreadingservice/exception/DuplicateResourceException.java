package com.voltaras.meterreadingservice.exception;

/**
 * Thrown when a resource creation violates a uniqueness constraint,
 * e.g. a duplicate meter reading for the same meter and billing period.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
