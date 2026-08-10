package com.voltaras.notificationservice.exception;

/**
 * Thrown when the authenticated user is not allowed to perform an
 * operation (for example an ADMIN-only notification API called with a
 * CONSUMER role).
 *
 * <p>
 * This service does not use Spring Security: authorization is enforced from
 * the {@code X-User-Role} header injected by the API Gateway and this
 * exception is mapped to {@code 403 FORBIDDEN} by the
 * {@code GlobalExceptionHandler}.
 * </p>
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
