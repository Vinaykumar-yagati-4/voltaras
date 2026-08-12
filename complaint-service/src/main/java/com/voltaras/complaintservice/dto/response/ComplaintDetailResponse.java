package com.voltaras.complaintservice.dto.response;

import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full complaint representation including comments and status history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ComplaintDetailResponse", description = "Full complaint with comments and status history")
public class ComplaintDetailResponse {

    @Schema(description = "Complaint ID", example = "5")
    private Long id;

    @Schema(description = "Unique ticket number", example = "CMP-20260812-0001")
    private String ticketNumber;

    @Schema(description = "Auth Service user ID of the complaint owner", example = "13")
    private Long consumerId;

    @Schema(description = "Category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Category name", example = "BILLING_ISSUE")
    private String categoryName;

    @Schema(description = "Complaint subject", example = "Incorrect bill amount for July 2026")
    private String subject;

    @Schema(description = "Complaint description")
    private String description;

    @Schema(description = "Lifecycle status", example = "OPEN")
    private ComplaintStatus status;

    @Schema(description = "Priority", example = "NORMAL")
    private ComplaintPriority priority;

    @Schema(description = "Assigned admin user ID, or null when unassigned", example = "2")
    private Long assignedTo;

    @Schema(description = "Resolution timestamp, set when RESOLVED", example = "2026-08-13T09:00:00")
    private LocalDateTime resolvedAt;

    @Schema(description = "Closing timestamp, set when CLOSED", example = "2026-08-14T09:00:00")
    private LocalDateTime closedAt;

    @Schema(description = "Creation timestamp", example = "2026-08-12T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-08-12T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Comments on the complaint, oldest first")
    private List<CommentResponse> comments;

    @Schema(description = "Status transition history, oldest first")
    private List<StatusHistoryResponse> statusHistory;
}
