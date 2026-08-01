package com.voltaras.meterreadingservice.exception;

/**
 * Thrown when a requested resource (e.g. a meter reading) cannot be found.
 * <p>
 * Message format follows the VOLTARAS convention:
 * {@code "MeterReading not found with id: '5'"}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
