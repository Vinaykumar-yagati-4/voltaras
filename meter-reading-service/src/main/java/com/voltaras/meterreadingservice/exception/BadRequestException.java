package com.voltaras.meterreadingservice.exception;

/**
 * Thrown when a request violates a business rule (e.g. current reading
 * is lower than the previous reading). Mapped to HTTP 400 BAD REQUEST.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
