package com.voltaras.paymentservice.exception;

/**
 * Thrown when the caller is not authorized to access a payment, bill or
 * organization, or when the Razorpay webhook signature is invalid.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
