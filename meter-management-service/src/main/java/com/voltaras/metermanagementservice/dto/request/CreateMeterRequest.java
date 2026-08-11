package com.voltaras.metermanagementservice.dto.request;

import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating a new physical meter (ADMIN only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CreateMeterRequest",
        description = "Payload for creating a new meter"
)
public class CreateMeterRequest {

    @NotBlank(message = "Meter number is required")
    @Size(max = 50, message = "Meter number must not exceed 50 characters")
    @Schema(description = "Unique meter number printed on the meter", example = "MTR-2026-0001")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    @Schema(description = "Technology type of the meter", example = "SMART")
    private MeterType meterType;

    @NotNull(message = "Connection type is required")
    @Schema(description = "Nature of the connection", example = "RESIDENTIAL")
    private ConnectionType connectionType;

    @NotNull(message = "Phase type is required")
    @Schema(description = "Electrical phase configuration", example = "SINGLE_PHASE")
    private PhaseType phaseType;

    @Schema(
            description = "Initial status. Defaults to ACTIVE when not provided",
            example = "ACTIVE"
    )
    private MeterStatus status;

    @NotNull(message = "Sanctioned load is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Sanctioned load must be positive"
    )
    @Schema(description = "Sanctioned load in kW (must be positive)", example = "5.0")
    private BigDecimal sanctionedLoadKw;

    @PastOrPresent(message = "Installation date cannot be in the future")
    @Schema(description = "Date the meter was installed", example = "2026-01-15")
    private LocalDate installationDate;

    @Size(max = 255, message = "Address line must not exceed 255 characters")
    @Schema(description = "Installation address line", example = "12, MG Road")
    private String addressLine;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City", example = "Bengaluru")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State", example = "Karnataka")
    private String state;

    @Pattern(
            regexp = "^\\d{6}$",
            message = "Pincode must be a valid 6-digit postal code"
    )
    @Schema(description = "6-digit postal code", example = "560001")
    private String pincode;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Schema(description = "Optional remarks", example = "Installed during AMI rollout")
    private String remarks;
}
