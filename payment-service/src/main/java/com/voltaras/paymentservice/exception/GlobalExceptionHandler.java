package com.voltaras.paymentservice.exception;

import com.voltaras.paymentservice.dto.response.ErrorResponse;
import com.voltaras.paymentservice.dto.response.ValidationErrorDetail;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts every exception into the standardized VOLTARAS
 * {@link ErrorResponse} envelope.
 */
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MISSING_PARAMETER",
                        "Required request parameter '" + ex.getParameterName() + "' is missing",
                        null, request));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("MISSING_HEADER",
                        "Required request header '" + ex.getHeaderName() + "' is missing",
                        null, request));
    }

    /**
     * Covers invalid enum values and malformed JSON bodies (for example an
     * unknown {@code paymentMethod} string).
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
     * Covers invalid enum values in query/request parameters.
     */
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

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            BusinessRuleException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("BUSINESS_RULE_VIOLATION", ex.getMessage(), null, request));
    }

    /**
     * Wallet balance lower than the requested bill payment amount.
     */
    @ExceptionHandler(InsufficientWalletBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientWalletBalance(
            InsufficientWalletBalanceException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("INSUFFICIENT_WALLET_BALANCE", ex.getMessage(), null, request));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateException(
            InvalidStateException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse("BAD_REQUEST", ex.getMessage(), null, request));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflictException(
            IdempotencyConflictException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse("IDEMPOTENCY_CONFLICT", ex.getMessage(), null, request));
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

    /**
     * The Auth Service reports that the authenticated user does not exist.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildResponse("USER_NOT_FOUND", ex.getMessage(), null, request));
    }

    /**
     * The authenticated user exists but is deactivated in the Auth Service.
     */
    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ErrorResponse> handleInactiveUserException(
            InactiveUserException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildResponse("USER_INACTIVE", ex.getMessage(), null, request));
    }

    /**
     * The user identity could not be verified with the Auth Service (user
     * ID or role mismatch with the gateway headers, or rejected token).
     */
    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedUserException(
            UnauthorizedUserException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                buildResponse("UNAUTHORIZED_USER", ex.getMessage(), null, request));
    }

    /**
     * Payment provider failure (unexpected provider behavior).
     */
    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProviderException(
            PaymentProviderException ex, HttpServletRequest request) {

        log.error("Payment provider failure on {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                buildResponse("PAYMENT_PROVIDER_ERROR", ex.getMessage(), null, request));
    }

    /**
     * Bill Service or Organization Service failure.
     */
    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamServiceException(
            UpstreamServiceException ex, HttpServletRequest request) {

        log.error("Upstream service failure on {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                buildResponse("UPSTREAM_SERVICE_ERROR", ex.getMessage(), null, request));
    }

    /**
     * Backstop for the payment_reference / idempotency_key unique
     * constraints under concurrency.
     */
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
