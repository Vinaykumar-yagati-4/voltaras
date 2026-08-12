package com.voltaras.complaintservice.exception;

import com.voltaras.complaintservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts every exception into the standardized VOLTARAS
 * {@link ErrorResponse} envelope. Stack traces are never exposed
 * to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Request body validation failures (jakarta.validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.ValidationErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.ValidationErrorDetail.builder()
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
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.ValidationErrorDetail> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.ValidationErrorDetail.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("VALIDATION_ERROR",
                        "Validation failed for the request", details, request));
    }

    /**
     * Missing identity/role headers (X-User-Id, X-User-Role) -&gt; 400.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MISSING_HEADER",
                        "Required request header '" + ex.getHeaderName() + "' is missing",
                        null, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MISSING_PARAMETER",
                        "Required request parameter '" + ex.getParameterName() + "' is missing",
                        null, request));
    }

    /**
     * Covers malformed JSON bodies and invalid enum values in the body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MALFORMED_REQUEST",
                        "Request body is missing, malformed or contains invalid enum values",
                        null, request));
    }

    /**
     * Covers invalid path/query variable types (for example a non-numeric
     * complaint ID or an unknown status filter value).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("INVALID_ARGUMENT",
                        "Invalid value for parameter: " + ex.getName(),
                        null, request));
    }

    /**
     * Missing resource lookups -&gt; 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildResponse("RESOURCE_NOT_FOUND", ex.getMessage(), null, request));
    }

    /**
     * Unmapped paths and missing static resources (Spring 6.1+) -&gt; 404 NOT
     * FOUND instead of falling through to the generic 500 handler.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildResponse("RESOURCE_NOT_FOUND",
                        "No handler found for " + request.getMethod()
                                + " " + request.getRequestURI(),
                        null, request));
    }

    /**
     * Business rule violations -&gt; 400 BAD REQUEST.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("BUSINESS_RULE_VIOLATION", ex.getMessage(), null, request));
    }

    /**
     * Unauthorized role or foreign resource -&gt; 403 FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildResponse("ACCESS_DENIED", ex.getMessage(), null, request));
    }

    /**
     * Database constraint violations (for example the unique ticket-number
     * constraint raced the pre-check) -&gt; 409 CONFLICT.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Data integrity violation on {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse("DATA_CONSTRAINT_VIOLATION",
                        "The request conflicts with existing data",
                        null, request));
    }

    /**
     * Fallback for any unhandled exception -&gt; 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred on {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildResponse("INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later.",
                        null, request));
    }

    private ErrorResponse buildResponse(
            String code, String message,
            List<ErrorResponse.ValidationErrorDetail> details,
            HttpServletRequest request) {

        return ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
    }
}
