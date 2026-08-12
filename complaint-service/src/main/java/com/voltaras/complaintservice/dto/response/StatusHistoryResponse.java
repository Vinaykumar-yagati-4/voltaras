package com.voltaras.complaintservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single recorded complaint status transition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "StatusHistoryResponse", description = "One recorded complaint status transition")
public class StatusHistoryResponse {

    @Schema(description = "Status before the transition (null for the initial OPEN entry)", example = "OPEN")
    private String fromStatus;

    @Schema(description = "Status after the transition", example = "IN_PROGRESS")
    private String toStatus;

    @Schema(description = "Auth Service user ID of the admin who changed the status", example = "2")
    private Long changedBy;

    @Schema(description = "Transition timestamp", example = "2026-08-13T09:00:00")
    private LocalDateTime changedAt;
}
