package com.voltaras.paymentservice.exception;

/**
 * Thrown when a request violates a payment business rule, for example
 * paying an already-paid bill or requesting an amount below the bill total.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
