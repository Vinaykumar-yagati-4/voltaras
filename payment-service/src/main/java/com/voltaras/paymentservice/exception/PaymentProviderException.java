package com.voltaras.paymentservice.exception;

/**
 * Thrown when the payment provider itself fails in an unexpected way
 * (for example returning a null or unknown result). Normal provider
 * outcomes such as a declined payment are represented by the FAILED
 * payment status, not by this exception.
 */
public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
