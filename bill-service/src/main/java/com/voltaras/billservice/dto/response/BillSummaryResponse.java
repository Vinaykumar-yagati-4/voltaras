package com.voltaras.billservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voltaras.billservice.enums.BillStatus;
import com.voltaras.billservice.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight bill representation used for list endpoints (history,
 * outstanding and admin filtering).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "BillSummaryResponse", description = "Summary of a bill for list views")
public class BillSummaryResponse {

    @Schema(description = "Bill ID", example = "1")
    private Long id;

    @Schema(description = "Consumer auth user ID", example = "100")
    private Long authUserId;

    @Schema(description = "Meter number", example = "MTR-2024-00123")
    private String meterNumber;

    @Schema(description = "Billing month (1-12)", example = "6")
    private Integer billingMonth;

    @Schema(description = "Billing year", example = "2026")
    private Integer billingYear;

    @Schema(description = "Units consumed in the billing period", example = "135.25")
    private BigDecimal unitsConsumed;

    @Schema(description = "Total payable amount", example = "353.99")
    private BigDecimal totalAmount;

    @Schema(description = "Amount paid so far", example = "0.00")
    private BigDecimal amountPaid;

    @Schema(description = "Remaining outstanding amount", example = "353.99")
    private BigDecimal outstandingAmount;

    @Schema(description = "Bill lifecycle status", example = "GENERATED")
    private BillStatus billStatus;

    @Schema(description = "Payment status", example = "UNPAID")
    private PaymentStatus paymentStatus;

    @Schema(description = "Date the bill was generated", example = "2026-06-01")
    private LocalDate generatedDate;

    @Schema(description = "Due date", example = "2026-06-16")
    private LocalDate dueDate;
}
