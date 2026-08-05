package com.voltaras.meterreadingservice.controller;

import com.voltaras.meterreadingservice.dto.request.RejectMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.UpdateMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.response.ErrorResponse;
import com.voltaras.meterreadingservice.dto.response.MeterReadingResponse;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import com.voltaras.meterreadingservice.service.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@Tag(
        name = "Meter Readings",
        description = """
                Consumer and admin APIs for submitting, viewing,
                updating, deleting, verifying and rejecting
                electricity meter readings.
                """
)
@SecurityRequirement(name = "bearerAuth")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @Operation(
            summary = "Submit a meter reading",
            description = """
                    Submits a new electricity meter reading for the
                    currently authenticated consumer.

                    The API Gateway validates the JWT and injects
                    the authenticated user ID through X-User-Id.

                    New readings are normally created with a pending status.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Meter reading submitted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MeterReadingResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid meter reading data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate meter reading for the same meter and billing period",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<MeterReadingResponse> submitReading(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody SubmitMeterReadingRequest request
    ) {

        MeterReadingResponse response =
                meterReadingService.submitReading(
                        authUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Get my meter readings",
            description = """
                    Returns all meter readings submitted by the
                    currently authenticated consumer.

                    The user identity is obtained from X-User-Id,
                    which is injected by the API Gateway.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter readings retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = MeterReadingResponse.class
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<List<MeterReadingResponse>> getMyReadings(
            @RequestHeader("X-User-Id") Long authUserId
    ) {

        List<MeterReadingResponse> response =
                meterReadingService.getMyReadings(authUserId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get my meter reading by ID",
            description = """
                    Returns one meter reading belonging to the currently
                    authenticated consumer.

                    A consumer cannot access another user's reading.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter reading retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MeterReadingResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Meter reading was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/me/{readingId}")
    public ResponseEntity<MeterReadingResponse> getMyReadingById(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long readingId
    ) {

        MeterReadingResponse response =
                meterReadingService.getMyReadingById(
                        authUserId,
                        readingId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update my meter reading",
            description = """
                    Updates a meter reading belonging to the currently
                    authenticated consumer.

                    Only readings that are still editable according to
                    the service business rules can be updated.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter reading updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MeterReadingResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid meter reading data or reading cannot be updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Meter reading was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/me/{readingId}")
    public ResponseEntity<MeterReadingResponse> updateMyReading(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long readingId,
            @Valid @RequestBody UpdateMeterReadingRequest request
    ) {

        MeterReadingResponse response =
                meterReadingService.updateMyReading(
                        authUserId,
                        readingId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete my meter reading",
            description = """
                    Deletes a meter reading belonging to the currently
                    authenticated consumer.

                    Only readings that are still deletable according to
                    the service business rules can be removed.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter reading deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Meter reading cannot be deleted",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Meter reading was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @DeleteMapping("/me/{readingId}")
    public ResponseEntity<Map<String, String>> deleteMyReading(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long readingId
    ) {

        meterReadingService.deleteMyReading(
                authUserId,
                readingId
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Meter reading deleted successfully"
                )
        );
    }

    @Operation(
            summary = "Get all meter readings for admin",
            description = """
                    Returns all meter readings for an authenticated admin.

                    Results can optionally be filtered by status.

                    The API Gateway injects the authenticated system role
                    through X-User-Role.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter readings retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = MeterReadingResponse.class
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid status parameter",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Admin role is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/admin")
    public ResponseEntity<List<MeterReadingResponse>> getAllReadingsForAdmin(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(
                    name = "status",
                    required = false
            ) MeterReadingStatus status
    ) {

        List<MeterReadingResponse> response =
                meterReadingService.getAllReadingsForAdmin(
                        role,
                        status
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verify a meter reading",
            description = """
                    Marks a meter reading as verified.

                    This operation is restricted to authenticated admins.
                    The admin user ID and role are supplied by the API Gateway.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter reading verified successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MeterReadingResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Meter reading cannot be verified",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Admin role is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Meter reading was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PatchMapping("/admin/{readingId}/verify")
    public ResponseEntity<MeterReadingResponse> verifyReading(
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long readingId
    ) {

        MeterReadingResponse response =
                meterReadingService.verifyReading(
                        adminUserId,
                        role,
                        readingId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reject a meter reading",
            description = """
                    Rejects a meter reading and records mandatory admin remarks.

                    This operation is restricted to authenticated admins.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter reading rejected successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MeterReadingResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid rejection request or reading cannot be rejected",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Admin role is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Meter reading was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PatchMapping("/admin/{readingId}/reject")
    public ResponseEntity<MeterReadingResponse> rejectReading(
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long readingId,
            @Valid @RequestBody RejectMeterReadingRequest request
    ) {

        MeterReadingResponse response =
                meterReadingService.rejectReading(
                        adminUserId,
                        role,
                        readingId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}