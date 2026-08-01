package com.voltaras.meterreadingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error envelope returned by the global exception handler.
 * <p>
 * Follows the VOLTARAS convention used across auth-service and
 * user-service:
 * <pre>
 * {
 *   "success": false,
 *   "error": { "code": "RESOURCE_NOT_FOUND", "message": "..." },
 *   "timestamp": "2026-07-31T10:15:30",
 *   "path": "/api/meter-readings/me/5"
 * }
 * </pre>
 * Field-specific validation failures are included under {@code error.details}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private ErrorDetail error;
    private LocalDateTime timestamp;
    private String path;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private List<FieldError> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
