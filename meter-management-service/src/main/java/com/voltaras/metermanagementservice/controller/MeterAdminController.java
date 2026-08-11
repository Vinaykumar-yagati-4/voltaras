package com.voltaras.metermanagementservice.controller;

import com.voltaras.metermanagementservice.dto.request.AssignMeterRequest;
import com.voltaras.metermanagementservice.dto.request.CreateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterStatusRequest;
import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.service.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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

import java.util.List;
import java.util.Map;

/**
 * ADMIN-only meter management APIs. The {@code X-User-Role} header must
 * carry {@code ADMIN} or {@code ROLE_ADMIN}, enforced in the service layer.
 */
@RestController
@RequestMapping("/api/meters/admin")
@RequiredArgsConstructor
@Tag(
        name = "Admin Meter APIs",
        description = "ADMIN-only meter management (X-User-Role must be ADMIN)."
)
@SecurityRequirement(name = "bearerAuth")
public class MeterAdminController {

    private final MeterService meterService;

    @PostMapping
    @Operation(
            summary = "Create a meter",
            description = "Registers a new physical meter. The meter number "
                    + "must be unique. Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Meter created",
                    content = @Content(
                            schema = @Schema(implementation = MeterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid meter data"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Meter number already exists")
    })
    public ResponseEntity<MeterResponse> createMeter(
            @Parameter(description = "Meter payload")
            @Valid @RequestBody CreateMeterRequest request,
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        MeterResponse response = meterService.createMeter(adminUserId, systemRole, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Get all meters (with optional filters)",
            description = "Returns all meters, optionally filtered by status, "
                    + "authUserId, organizationId or meterNumber. Requires "
                    + "X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meters retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = MeterSummaryResponse.class
                                    )
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid filter value"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role")
    })
    public ResponseEntity<List<MeterSummaryResponse>> getAllMetersForAdmin(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Optional status filter", example = "ACTIVE")
            @RequestParam(required = false) MeterStatus status,
            @Parameter(description = "Optional consumer filter", example = "100")
            @RequestParam(name = "authUserId", required = false) Long authUserId,
            @Parameter(description = "Optional organization filter", example = "7")
            @RequestParam(name = "organizationId", required = false) Long organizationId,
            @Parameter(description = "Optional meter number filter", example = "MTR-2026-0001")
            @RequestParam(name = "meterNumber", required = false) String meterNumber) {

        List<MeterSummaryResponse> response = meterService.getAllMetersForAdmin(
                systemRole, status, authUserId, organizationId, meterNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a meter by ID",
            description = "Returns the full details of one meter. Requires "
                    + "X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter retrieved",
                    content = @Content(
                            schema = @Schema(implementation = MeterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Meter not found")
    })
    public ResponseEntity<MeterResponse> getMeterByIdForAdmin(
            @Parameter(description = "Meter ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        return ResponseEntity.ok(meterService.getMeterByIdForAdmin(systemRole, id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update meter details",
            description = "Updates the editable details of a meter. Only the "
                    + "provided fields are changed; meterNumber is immutable. "
                    + "Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter updated",
                    content = @Content(
                            schema = @Schema(implementation = MeterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid meter data"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Meter not found")
    })
    public ResponseEntity<MeterResponse> updateMeter(
            @Parameter(description = "Meter ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated meter fields")
            @Valid @RequestBody UpdateMeterRequest request,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        return ResponseEntity.ok(meterService.updateMeter(systemRole, id, request));
    }

    @PatchMapping("/{id}/assign")
    @Operation(
            summary = "Assign a meter to a user",
            description = "Assigns the meter to the consumer identified by "
                    + "authUserId (required) and optionally links it to an "
                    + "organization. Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter assigned",
                    content = @Content(
                            schema = @Schema(implementation = MeterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid assignment request"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Meter not found")
    })
    public ResponseEntity<MeterResponse> assignMeter(
            @Parameter(description = "Meter ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Assignment payload")
            @Valid @RequestBody AssignMeterRequest request,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        return ResponseEntity.ok(meterService.assignMeter(systemRole, id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update meter status",
            description = "Changes the status of a meter (for example marks a "
                    + "meter as FAULTY or REPLACED). Requires "
                    + "X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter status updated",
                    content = @Content(
                            schema = @Schema(implementation = MeterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid status request"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Meter not found")
    })
    public ResponseEntity<MeterResponse> updateMeterStatus(
            @Parameter(description = "Meter ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Status payload")
            @Valid @RequestBody UpdateMeterStatusRequest request,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        return ResponseEntity.ok(meterService.updateMeterStatus(systemRole, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove a meter (soft delete)",
            description = "Soft-deletes the meter by changing its status to "
                    + "REMOVED. The record is kept in the database. Requires "
                    + "X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meter removed"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Meter not found")
    })
    public ResponseEntity<Map<String, String>> removeMeter(
            @Parameter(description = "Meter ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        meterService.removeMeter(systemRole, id);

        return ResponseEntity.ok(Map.of(
                "message", "Meter removed successfully"));
    }
}
