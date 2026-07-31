package com.voltaras.userservice.exception;

import com.voltaras.userservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handling that converts exceptions into a
 * standardized {@link ErrorResponse} JSON envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles request body validation failures (jakarta.validation).
     * Returns all offending fields with their individual messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("VALIDATION_ERROR")
                        .message("Validation failed for the request")
                        .details(fieldErrors)
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles duplicate profile creation attempts -> HTTP 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("DUPLICATE_RESOURCE")
                        .message(ex.getMessage())
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles missing profile lookups -> HTTP 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("RESOURCE_NOT_FOUND")
                        .message(ex.getMessage())
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles malformed JSON request bodies -> HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("MALFORMED_REQUEST")
                        .message("Request body is missing or malformed")
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles invalid path/query variable types (e.g. non-numeric
     * authUserId) -> HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("INVALID_ARGUMENT")
                        .message("Invalid value for parameter: " + ex.getName())
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Fallback for any unhandled exception -> HTTP 500 INTERNAL SERVER ERROR.
     * Internal exception details are never leaked to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("INTERNAL_ERROR")
                        .message("An unexpected error occurred. Please try again later.")
                        .build())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
