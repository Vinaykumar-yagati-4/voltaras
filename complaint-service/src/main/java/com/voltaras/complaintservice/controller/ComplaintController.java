package com.voltaras.complaintservice.controller;

import com.voltaras.complaintservice.dto.request.AddComplaintCommentRequest;
import com.voltaras.complaintservice.dto.request.CreateComplaintRequest;
import com.voltaras.complaintservice.dto.request.UpdateComplaintRequest;
import com.voltaras.complaintservice.dto.response.CategoryResponse;
import com.voltaras.complaintservice.dto.response.CommentResponse;
import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.ComplaintSummaryResponse;
import com.voltaras.complaintservice.dto.response.ErrorResponse;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Consumer complaint APIs and shared lookups.
 *
 * <p>
 * The authenticated user identity is read from the {@code X-User-Id} /
 * {@code X-User-Role} headers injected by the API Gateway and is never
 * taken from the request body.
 * </p>
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Tag(
        name = "Complaint APIs",
        description = "Consumer complaint management and shared lookups."
)
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @Operation(
            summary = "Raise a new complaint",
            description = """
                    Creates a complaint for the authenticated consumer with an
                    auto-generated ticket number (CMP-YYYYMMDD-NNNN), status OPEN
                    and priority NORMAL. Requires X-User-Role = CONSUMER.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Complaint created",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class)),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the created complaint"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or inactive/unknown category",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires CONSUMER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> createComplaint(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Valid @RequestBody CreateComplaintRequest request) {

        ComplaintDetailResponse response =
                complaintService.createComplaint(authUserId, systemRole, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/complaints/" + response.getId())
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "List my complaints (paginated)",
            description = "Returns the authenticated consumer's own complaints, " +
                    "newest first, with optional status/priority/category filters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaints retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    public ResponseEntity<Page<ComplaintSummaryResponse>> getMyComplaints(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Filter by status", example = "OPEN")
            @RequestParam(required = false) ComplaintStatus status,
            @Parameter(description = "Filter by priority", example = "NORMAL")
            @RequestParam(required = false) ComplaintPriority priority,
            @Parameter(description = "Filter by category ID", example = "1")
            @RequestParam(required = false) Long categoryId,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(complaintService.getMyComplaints(
                authUserId, status, priority, categoryId, pageable));
    }

    @GetMapping("/{complaintId}")
    @Operation(
            summary = "Get my complaint by ID",
            description = "Returns the complaint with its comments and status history " +
                    "only when it belongs to the authenticated consumer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaint retrieved",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found or not owned by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> getMyComplaint(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId) {

        return ResponseEntity.ok(
                complaintService.getMyComplaint(authUserId, complaintId));
    }

    @GetMapping("/ticket/{ticketNumber}")
    @Operation(
            summary = "Get my complaint by ticket number",
            description = "Returns the complaint only when it belongs to the " +
                    "authenticated consumer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaint retrieved",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found or not owned by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> getMyComplaintByTicketNumber(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Ticket number", example = "CMP-20260812-0001")
            @PathVariable String ticketNumber) {

        return ResponseEntity.ok(
                complaintService.getMyComplaintByTicketNumber(authUserId, ticketNumber));
    }

    @PutMapping("/{complaintId}")
    @Operation(
            summary = "Edit my complaint (OPEN only)",
            description = "Updates the subject and description of the caller's " +
                    "complaint while it is still OPEN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Complaint updated",
                    content = @Content(schema = @Schema(implementation = ComplaintDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or complaint is no longer OPEN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Complaint not found or not owned by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ComplaintDetailResponse> updateMyComplaint(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId,
            @Valid @RequestBody UpdateComplaintRequest request) {

        return ResponseEntity.ok(
                complaintService.updateMyComplaint(authUserId, complaintId, request));
    }

    @PostMapping("/{complaintId}/comments")
    @Operation(
            summary = "Add a comment to my complaint",
            description = "Adds a consumer comment to the caller's own complaint. " +
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
                    responseCode = "404",
                    description = "Complaint not found or not owned by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CommentResponse> addConsumerComment(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Complaint ID", example = "5")
            @PathVariable Long complaintId,
            @Valid @RequestBody AddComplaintCommentRequest request) {

        CommentResponse response = complaintService.addConsumerComment(
                authUserId, complaintId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/categories")
    @Operation(
            summary = "List complaint categories",
            description = "Returns the active complaint categories (seeded at " +
                    "startup), used to fill the complaint form."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Categories retrieved",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))
                    )
            )
    })
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(complaintService.getActiveCategories());
    }

    @GetMapping("/internal/count")
    @Operation(
            summary = "Get complaint counts by status (internal)",
            description = "Returns the number of complaints per status for the admin " +
                    "dashboard. Requires X-User-Role = ADMIN; until the Dashboard " +
                    "Service exists this is restricted to the existing admin role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Counts retrieved",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Map<ComplaintStatus, Long>> getStatusCounts(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        return ResponseEntity.ok(complaintService.getStatusCounts(systemRole));
    }
}
