package com.voltaras.metermanagementservice.controller;

import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Meter APIs of the authenticated user. All reads are scoped to the
 * {@code X-User-Id} header injected by the API Gateway.
 */
@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(
        name = "Meter APIs",
        description = "Meters assigned to the authenticated user (X-User-Id)."
)
@SecurityRequirement(name = "bearerAuth")
public class MeterController {

    private final MeterService meterService;

    @GetMapping
    @Operation(
            summary = "Get my meters",
            description = "Returns the meters assigned to the authenticated "
                    + "user, newest first. The user identity comes from the "
                    + "X-User-Id header injected by the API Gateway."
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
            )
    })
    public ResponseEntity<List<MeterSummaryResponse>> getMyMeters(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(meterService.getMyMeters(authUserId));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get my meter by ID",
            description = "Returns one meter only if it is assigned to the "
                    + "authenticated user. A meter owned by another user is "
                    + "not accessible."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Meter retrieved",
                    content = @Content(
                            schema = @Schema(implementation = MeterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Meter not found or not owned by the user")
    })
    public ResponseEntity<MeterResponse> getMyMeterById(
            @Parameter(description = "Meter ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(meterService.getMyMeterById(authUserId, id));
    }
}
