package com.voltaras.complaintservice.dto.response;

import com.voltaras.complaintservice.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of an admin status transition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "StatusUpdateResponse", description = "Result of a complaint status transition")
public class StatusUpdateResponse {

    @Schema(description = "Complaint ID", example = "5")
    private Long complaintId;

    @Schema(description = "Unique ticket number", example = "CMP-20260812-0001")
    private String ticketNumber;

    @Schema(description = "Status before the transition", example = "OPEN")
    private ComplaintStatus previousStatus;

    @Schema(description = "Status after the transition", example = "IN_PROGRESS")
    private ComplaintStatus currentStatus;

    @Schema(description = "Timestamp of the transition", example = "2026-08-13T09:00:00")
    private LocalDateTime updatedAt;
}
