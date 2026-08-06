package com.voltaras.billservice.exception;

/**
 * Thrown when a request violates a bill business rule, for example:
 * current reading below previous reading, invalid billing period,
 * cancelling a PAID bill, or paying a CANCELLED bill.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
