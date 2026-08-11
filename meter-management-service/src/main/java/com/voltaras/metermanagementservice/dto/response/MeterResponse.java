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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full API response for a single meter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "MeterResponse",
        description = "Complete meter details"
)
public class MeterResponse {

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

    @Schema(description = "Date the meter was installed", example = "2026-01-15")
    private LocalDate installationDate;

    @Schema(description = "Installation address line", example = "12, MG Road")
    private String addressLine;

    @Schema(description = "City", example = "Bengaluru")
    private String city;

    @Schema(description = "State", example = "Karnataka")
    private String state;

    @Schema(description = "6-digit postal code", example = "560001")
    private String pincode;

    @Schema(description = "Optional remarks")
    private String remarks;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;
}
