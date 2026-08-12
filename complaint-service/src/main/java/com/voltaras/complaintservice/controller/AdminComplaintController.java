package com.voltaras.complaintservice.controller;

import com.voltaras.complaintservice.dto.request.AddComplaintCommentRequest;
import com.voltaras.complaintservice.dto.request.AssignComplaintRequest;
import com.voltaras.complaintservice.dto.request.UpdateComplaintStatusRequest;
import com.voltaras.complaintservice.dto.response.CommentResponse;
import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.ComplaintSummaryResponse;
import com.voltaras.complaintservice.dto.response.ErrorResponse;
import com.voltaras.complaintservice.dto.response.StatusUpdateResponse;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * ADMIN-only complaint management APIs. The {@code X-User-Role} header must
 * carry exactly {@code ADMIN} (the gateway's role format), enforced in the
 * service layer.
 */
@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
@Tag(
        name = "Admin Complaint APIs",
        description = "ADMIN-only complaint management (X-User-Role must be ADMIN)."
)
@SecurityRequirement(name = "bearerAuth")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    @Operation(
            summary = "List all complaints (paginated)",
            description = "Returns all complaints with optional filters " +
                    "(status, priority, category, consumer, assignee, date range)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaints retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Page<ComplaintSummaryResponse>> getAllComplaints(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Filter by status", example = "IN_PROGRESS")
            @RequestParam(required = false) ComplaintStatus status,
            @Parameter(description = "Filter by priority", example = "HIGH")
            @RequestParam(required = false) ComplaintPriority priority,
            @Parameter(description = "Filter by category ID", example = "1")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by complaint owner", example = "13")
            @RequestParam(required = false) Long consumerId,
            @Parameter(description = "Filter by assigned admin", example = "2")
            @RequestParam(required = false) Long assignedTo,
            @Parameter(description = "Filter from creation date (inclusive)", example = "2026-08-01")
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Filter to creation date (inclusive)", example = "2026-08-31")
            @RequestParam(required = false) LocalDate toDate,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(complaintService.getAllComplaintsForAdmin(
                systemRole, status, priority, categoryId, consumerId,
                assignedTo, fromDate, toDate, pageable));
    }

    @GetMapping("/{complaintId}")
    @Operation(
            summary = "Get complaint details for admin",
            description = "Returns any complaint with its comments and status history."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaint retrieved",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> getComplaintForAdmin(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId) {

        return ResponseEntity.ok(
                complaintService.getComplaintForAdmin(systemRole, complaintId));
    }

    @GetMapping("/ticket/{ticketNumber}")
    @Operation(
            summary = "Get complaint by ticket number for admin",
            description = "Returns any complaint by its ticket number."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaint retrieved",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> getComplaintByTicketNumberForAdmin(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Ticket number", example = "CMP-20260812-0001")
            @PathVariable String ticketNumber) {

        return ResponseEntity.ok(
                complaintService.getComplaintByTicketNumberForAdmin(systemRole, ticketNumber));
    }

    @PatchMapping("/{complaintId}/status")
    @Operation(
            summary = "Update complaint status",
            description = """
                    Moves a complaint to a new lifecycle status. Allowed transitions:
                    OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED. Each transition is
                    recorded in the status history and publishes a
                    ComplaintStatusChangedEvent to the Notification Service.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status updated",
                    content = @Content(schema = @Schema(implementation = StatusUpdateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid, same-status or terminal transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<StatusUpdateResponse> updateComplaintStatus(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId,
            @Valid @RequestBody UpdateComplaintStatusRequest request) {

        return ResponseEntity.ok(complaintService.updateComplaintStatus(
                systemRole, adminUserId, complaintId, request));
    }

    @PutMapping("/{complaintId}/assign")
    @Operation(
            summary = "Assign a complaint",
            description = "Assigns a complaint to an admin. Only allowed while the " +
                    "complaint is OPEN or IN_PROGRESS."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaint assigned",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Complaint cannot be assigned in its current state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> assignComplaint(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId,
            @Valid @RequestBody AssignComplaintRequest request) {

        return ResponseEntity.ok(complaintService.assignComplaint(
                systemRole, adminUserId, complaintId, request));
    }

    @PostMapping("/{complaintId}/comments")
    @Operation(
            summary = "Add an admin resolution comment",
            description = "Adds an admin resolution comment to a complaint. " +
                    "Comments are not allowed on CLOSED complaints."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Comment added",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or complaint is CLOSED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CommentResponse> addAdminComment(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId,
            @Valid @RequestBody AddComplaintCommentRequest request) {

        CommentResponse response = complaintService.addAdminComment(
                systemRole, adminUserId, complaintId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
