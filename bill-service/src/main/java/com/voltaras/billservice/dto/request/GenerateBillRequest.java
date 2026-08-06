package com.voltaras.billservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for generating a new electricity bill (admin only).
 *
 * <p>
 * The bill owner is never read from the request body: it always comes
 * from the X-User-Id header injected by the API Gateway.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "GenerateBillRequest",
        description = "Payload used by administrators to generate a bill"
)
public class GenerateBillRequest {

    @NotNull
    @Schema(
            description = "ID of the verified meter reading this bill is generated from",
            example = "42"
    )
    private Long meterReadingId;

    @NotBlank
    @Schema(description = "Meter number of the consumer", example = "MTR-2024-00123")
    private String meterNumber;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Previous meter reading", example = "1250.50")
    private BigDecimal previousReading;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Current meter reading, must be >= previousReading", example = "1385.75")
    private BigDecimal currentReading;

    @NotNull
    @Min(1)
    @Max(12)
    @Schema(description = "Billing month (1-12)", example = "6")
    private Integer billingMonth;

    @NotNull
    @Schema(description = "Billing year", example = "2026")
    private Integer billingYear;

    @Schema(
            description = "Date the bill was generated. Defaults to today when omitted",
            example = "2026-06-01"
    )
    private LocalDate generatedDate;

    @NotNull
    @Schema(
            description = "Due date, must be after the generated date",
            example = "2026-06-16"
    )
    private LocalDate dueDate;

    @Schema(description = "Optional remarks attached to the bill", example = "Generated from verified reading")
    private String remarks;
}
