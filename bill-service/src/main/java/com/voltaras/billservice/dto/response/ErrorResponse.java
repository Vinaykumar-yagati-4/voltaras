package com.voltaras.billservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized VOLTARAS error envelope used across all services.
 *
 * <pre>
 * {
 *   "success": false,
 *   "error": {
 *     "code": "RESOURCE_NOT_FOUND",
 *     "message": "...",
 *     "details": []
 *   },
 *   "timestamp": "...",
 *   "path": "/api/bills/..."
 * }
 * </pre>
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
        private List<ValidationErrorDetail> details;
    }
}
