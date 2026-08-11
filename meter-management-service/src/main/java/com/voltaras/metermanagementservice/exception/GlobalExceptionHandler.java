package com.voltaras.metermanagementservice.exception;

import com.voltaras.metermanagementservice.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts every exception into the standardized VOLTARAS
 * {@link ApiErrorResponse} envelope. Stack traces are never exposed
 * to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Request body validation failures (jakarta.validation).
     * Returns all offending fields with their individual messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorResponse.ValidationErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ApiErrorResponse.ValidationErrorDetail.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("VALIDATION_ERROR",
                        "Validation failed for the request", details, request));
    }

    /**
     * Path/query parameter constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ApiErrorResponse.ValidationErrorDetail> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> ApiErrorResponse.ValidationErrorDetail.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("VALIDATION_ERROR",
                        "Validation failed for the request", details, request));
    }

    /**
     * Missing identity/role headers (X-User-Id, X-User-Role) -> 400.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MISSING_HEADER",
                        "Required request header '" + ex.getHeaderName() + "' is missing",
                        null, request));
    }

    /**
     * Covers malformed JSON bodies and invalid enum values in the body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MALFORMED_REQUEST",
                        "Request body is missing, malformed or contains invalid enum values",
                        null, request));
    }

    /**
     * Covers invalid path/query variable types (for example a non-numeric
     * meter ID or an unknown status filter value).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("INVALID_ARGUMENT",
                        "Invalid value for parameter: " + ex.getName(),
                        null, request));
    }

    /**
     * Missing resource lookups -> 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildResponse("RESOURCE_NOT_FOUND", ex.getMessage(), null, request));
    }

    /**
     * Duplicate meter number -> 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse("DUPLICATE_RESOURCE", ex.getMessage(), null, request));
    }

    /**
     * Business rule violations -> 400 BAD REQUEST.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("BAD_REQUEST", ex.getMessage(), null, request));
    }

    /**
     * Unauthorized role -> 403 FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildResponse("ACCESS_DENIED", ex.getMessage(), null, request));
    }

    /**
     * Database constraint violations (e.g. the unique meter-number
     * constraint raced the pre-check) -> 409 CONFLICT.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Data integrity violation on {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse("DATA_CONSTRAINT_VIOLATION",
                        "Duplicate meter number or database constraint violation",
                        null, request));
    }

    /**
     * Fallback for any unhandled exception -> 500 INTERNAL SERVER ERROR.
     * Internal exception details are never leaked to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred on {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildResponse("INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later.",
                        null, request));
    }

    private ApiErrorResponse buildResponse(
            String code, String message,
            List<ApiErrorResponse.ValidationErrorDetail> details,
            HttpServletRequest request) {

        return ApiErrorResponse.builder()
                .success(false)
                .error(ApiErrorResponse.ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
    }
}
