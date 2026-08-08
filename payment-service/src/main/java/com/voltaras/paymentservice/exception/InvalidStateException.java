package com.voltaras.paymentservice.exception;

/**
 * Thrown when a payment status transition is not allowed, for example
 * cancelling a SUCCESS payment or retrying a CANCELLED payment.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }
}
