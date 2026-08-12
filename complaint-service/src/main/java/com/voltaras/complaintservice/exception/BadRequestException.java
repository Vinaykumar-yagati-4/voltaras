package com.voltaras.complaintservice.exception;

/**
 * Thrown for business-rule violations (for example an invalid status
 * transition or a comment on a closed complaint). Mapped to
 * {@code 400 BUSINESS_RULE_VIOLATION}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
