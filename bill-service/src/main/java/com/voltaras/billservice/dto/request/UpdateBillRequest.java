package com.voltaras.billservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload used by administrators to safely update a bill.
 *
 * <p>
 * Only due date, late fee, discount and remarks are updatable. Late fee
 * and discount changes trigger a recalculation of the total amount in the
 * service layer. Null fields are ignored (kept unchanged).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UpdateBillRequest",
        description = "Payload used by administrators to update a bill"
)
public class UpdateBillRequest {

    @Schema(
            description = "New due date, must be after the generated date",
            example = "2026-06-30"
    )
    private LocalDate dueDate;

    @PositiveOrZero
    @Schema(description = "Late fee applied to the bill", example = "50.00")
    private BigDecimal lateFee;

    @PositiveOrZero
    @Schema(description = "Discount applied to the bill", example = "25.00")
    private BigDecimal discountAmount;

    @Schema(description = "Updated remarks", example = "Late fee waived by administrator")
    private String remarks;
}
