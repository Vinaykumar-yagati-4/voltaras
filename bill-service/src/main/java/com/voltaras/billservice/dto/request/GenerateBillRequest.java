package com.voltaras.billservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for generating a new electricity bill (Admin only).
 *
 * The consumer ID is supplied through authUserId in the request body.
 * The authenticated Admin ID comes from the X-User-Id header injected
 * by the API Gateway and is stored separately as generatedBy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "GenerateBillRequest",
        description = "Payload used by administrators to generate a consumer bill"
)
public class GenerateBillRequest {

    @NotNull
    @Positive
    @Schema(
            description = "Auth user ID of the consumer who owns the bill",
            example = "13"
    )
    private Long authUserId;

    @NotNull
    @Positive
    @Schema(
            description = "ID of the verified meter reading used to generate the bill",
            example = "6"
    )
    private Long meterReadingId;

    @NotBlank
    @Schema(
            description = "Meter number belonging to the consumer",
            example = "GVR-0001"
    )
    private String meterNumber;

    @NotNull
    @PositiveOrZero
    @Schema(
            description = "Previous meter reading",
            example = "1250.00"
    )
    private BigDecimal previousReading;

    @NotNull
    @PositiveOrZero
    @Schema(
            description = "Current meter reading; must be greater than or equal to the previous reading",
            example = "1325.00"
    )
    private BigDecimal currentReading;

    @NotNull
    @Min(1)
    @Max(12)
    @Schema(
            description = "Billing month from 1 to 12",
            example = "8"
    )
    private Integer billingMonth;

    @NotNull
    @Schema(
            description = "Billing year",
            example = "2026"
    )
    private Integer billingYear;

    @Schema(
            description = "Date the bill was generated; defaults to the current date when omitted",
            example = "2026-08-07"
    )
    private LocalDate generatedDate;

    @NotNull
    @Schema(
            description = "Payment due date; must be after the generated date",
            example = "2026-08-25"
    )
    private LocalDate dueDate;

    @Schema(
            description = "Optional remarks attached to the bill",
            example = "Generated from verified meter reading 6"
    )
    private String remarks;
}