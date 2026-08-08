package com.voltaras.paymentservice.exception;

/**
 * Thrown when an upstream service (Bill Service or Organization Service)
 * cannot be reached or returns an unexpected response. Mapped to HTTP 502
 * with the UPSTREAM_SERVICE_ERROR error code.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
