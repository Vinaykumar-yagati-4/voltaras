package com.voltaras.metermanagementservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight API response used by list endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "MeterSummaryResponse",
        description = "Lightweight meter summary used in list responses"
)
public class MeterSummaryResponse {

    @Schema(description = "Meter ID", example = "1")
    private Long id;

    @Schema(description = "Unique meter number", example = "MTR-2026-0001")
    private String meterNumber;

    @Schema(description = "Consumer the meter is assigned to", example = "100")
    private Long authUserId;

    @Schema(description = "Organization the meter belongs to", example = "7")
    private Long organizationId;

    @Schema(description = "Technology type of the meter", example = "SMART")
    private MeterType meterType;

    @Schema(description = "Nature of the connection", example = "RESIDENTIAL")
    private ConnectionType connectionType;

    @Schema(description = "Electrical phase configuration", example = "SINGLE_PHASE")
    private PhaseType phaseType;

    @Schema(description = "Current meter status", example = "ACTIVE")
    private MeterStatus status;

    @Schema(description = "Sanctioned load in kW", example = "5.0")
    private BigDecimal sanctionedLoadKw;

    @Schema(description = "City", example = "Bengaluru")
    private String city;
}
