package com.voltaras.organizationservice.exception;

import com.voltaras.organizationservice.dto.response.ErrorResponse;
import com.voltaras.organizationservice.dto.response.ValidationErrorDetail;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ValidationErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ValidationErrorDetail.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("VALIDATION_ERROR", "Validation failed for the request",
                        details, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ValidationErrorDetail> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> ValidationErrorDetail.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("VALIDATION_ERROR", "Validation failed for the request",
                        details, request));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MISSING_HEADER",
                        "Required request header '" + ex.getHeaderName() + "' is missing",
                        null, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MALFORMED_REQUEST", "Request body is missing or malformed",
                        null, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("INVALID_ARGUMENT",
                        "Invalid value for parameter: " + ex.getName(),
                        null, request));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("BAD_REQUEST", ex.getMessage(), null, request));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateException(
            InvalidStateException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("BAD_REQUEST", ex.getMessage(), null, request));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse("DUPLICATE_RESOURCE", ex.getMessage(), null, request));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildResponse("RESOURCE_NOT_FOUND", ex.getMessage(), null, request));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenOperationException(
            ForbiddenOperationException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildResponse("FORBIDDEN_OPERATION", ex.getMessage(), null, request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Data integrity violation on {}: {}",
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse("DATA_CONSTRAINT_VIOLATION",
                        "The request conflicts with existing data",
                        null, request));
    }

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
            String code, String message, List<ValidationErrorDetail> details,
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
