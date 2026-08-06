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
import java.time.LocalDateTime;

/**
 * Full bill representation returned to callers. Never exposes the JPA
 * entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "BillResponse", description = "Complete bill details")
public class BillResponse {

    @Schema(description = "Bill ID", example = "1")
    private Long id;

    @Schema(description = "Consumer auth user ID", example = "100")
    private Long authUserId;

    @Schema(description = "Source meter reading ID", example = "42")
    private Long meterReadingId;

    @Schema(description = "Meter number", example = "MTR-2024-00123")
    private String meterNumber;

    @Schema(description = "Billing month (1-12)", example = "6")
    private Integer billingMonth;

    @Schema(description = "Billing year", example = "2026")
    private Integer billingYear;

    @Schema(description = "Previous meter reading", example = "1250.50")
    private BigDecimal previousReading;

    @Schema(description = "Current meter reading", example = "1385.75")
    private BigDecimal currentReading;

    @Schema(description = "Units consumed in the billing period", example = "135.25")
    private BigDecimal unitsConsumed;

    @Schema(description = "Energy charge from tariff slabs", example = "237.13")
    private BigDecimal energyCharge;

    @Schema(description = "Fixed charge", example = "100.00")
    private BigDecimal fixedCharge;

    @Schema(description = "Tax amount", example = "16.86")
    private BigDecimal taxAmount;

    @Schema(description = "Late fee applied", example = "0.00")
    private BigDecimal lateFee;

    @Schema(description = "Discount applied", example = "0.00")
    private BigDecimal discountAmount;

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

    @Schema(description = "When the bill was paid", example = "2026-06-10T14:30:00")
    private LocalDateTime paidAt;

    @Schema(description = "Remarks")
    private String remarks;

    @Schema(description = "Admin auth user ID who generated the bill", example = "1")
    private Long generatedBy;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
