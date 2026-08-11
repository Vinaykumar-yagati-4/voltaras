package com.voltaras.metermanagementservice.exception;

/**
 * Thrown when a requested resource (e.g. a meter) cannot be found.
 *
 * <p>
 * Message format follows the VOLTARAS convention:
 * {@code "Meter not found with id: '5'"}.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
