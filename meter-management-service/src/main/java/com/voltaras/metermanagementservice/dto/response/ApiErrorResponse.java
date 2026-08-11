package com.voltaras.metermanagementservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized VOLTARAS error envelope returned by the
 * {@code GlobalExceptionHandler} for every failed request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ApiErrorResponse",
        description = "Standard error envelope returned on failed requests"
)
public class ApiErrorResponse {

    @Schema(description = "Always false on errors", example = "false")
    private boolean success;

    @Schema(description = "Error details")
    private ErrorDetail error;

    @Schema(description = "Timestamp of the failure", example = "2026-08-08T10:16:01")
    private LocalDateTime timestamp;

    @Schema(description = "Request path that failed", example = "/api/meters")
    private String path;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ErrorDetail", description = "Machine-readable error information")
    public static class ErrorDetail {

        @Schema(description = "Stable error code", example = "RESOURCE_NOT_FOUND")
        private String code;

        @Schema(description = "Human-readable error message")
        private String message;

        @Schema(
                description = "Field-level validation errors, when applicable",
                nullable = true
        )
        private List<ValidationErrorDetail> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ValidationErrorDetail", description = "A single field validation error")
    public static class ValidationErrorDetail {

        @Schema(description = "Field that failed validation", example = "meterNumber")
        private String field;

        @Schema(description = "Validation message", example = "Meter number is required")
        private String message;
    }
}
