package com.voltaras.metermanagementservice.dto.request;

import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
 * Payload for updating meter details (ADMIN only).
 *
 * <p>
 * All fields are optional; only the provided (non-null) fields are applied.
 * The meter number is immutable and therefore not part of this payload.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UpdateMeterRequest",
        description = "Payload for updating meter details; only provided fields are applied"
)
public class UpdateMeterRequest {

    @Schema(description = "New meter type", example = "SMART")
    private MeterType meterType;

    @Schema(description = "New connection type", example = "COMMERCIAL")
    private ConnectionType connectionType;

    @Schema(description = "New phase type", example = "THREE_PHASE")
    private PhaseType phaseType;

    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Sanctioned load must be positive"
    )
    @Schema(description = "Sanctioned load in kW (must be positive)", example = "10.0")
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
    @Schema(description = "Optional remarks", example = "Load increased after renovation")
    private String remarks;
}
