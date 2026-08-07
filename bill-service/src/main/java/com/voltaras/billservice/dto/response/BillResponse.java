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
 * Full bill representation returned to callers.
 * Never exposes the JPA entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "BillResponse",
        description = "Complete bill details"
)
public class BillResponse {

    @Schema(description = "Bill ID", example = "1")
    private Long id;

    @Schema(description = "Consumer auth user ID", example = "13")
    private Long authUserId;

    @Schema(description = "Source meter reading ID", example = "6")
    private Long meterReadingId;

    @Schema(description = "Meter number", example = "GVR-0001")
    private String meterNumber;

    @Schema(description = "Billing month (1-12)", example = "8")
    private Integer billingMonth;

    @Schema(description = "Billing year", example = "2026")
    private Integer billingYear;

    @Schema(description = "Previous meter reading", example = "1250.00")
    private BigDecimal previousReading;

    @Schema(description = "Current meter reading", example = "1325.00")
    private BigDecimal currentReading;

    @Schema(
            description = "Units consumed during the billing period",
            example = "75.00"
    )
    private BigDecimal unitsConsumed;

    @Schema(
            description = "Energy charge calculated from tariff slabs",
            example = "112.50"
    )
    private BigDecimal energyCharge;

    @Schema(description = "Fixed charge", example = "100.00")
    private BigDecimal fixedCharge;

    @Schema(description = "Tax amount", example = "10.63")
    private BigDecimal taxAmount;

    @Schema(description = "Late fee applied", example = "0.00")
    private BigDecimal lateFee;

    @Schema(description = "Total payable amount", example = "223.13")
    private BigDecimal totalAmount;

    @Schema(description = "Amount paid so far", example = "0.00")
    private BigDecimal amountPaid;

    @Schema(
            description = "Remaining outstanding amount",
            example = "223.13"
    )
    private BigDecimal outstandingAmount;

    @Schema(description = "Bill lifecycle status", example = "GENERATED")
    private BillStatus billStatus;

    @Schema(description = "Payment status", example = "UNPAID")
    private PaymentStatus paymentStatus;

    @Schema(
            description = "Date the bill was generated",
            example = "2026-08-06"
    )
    private LocalDate generatedDate;

    @Schema(description = "Due date", example = "2026-08-25")
    private LocalDate dueDate;

    @Schema(
            description = "Date and time when payment was completed. "
                    + "Null or omitted until the bill is fully paid.",
            nullable = true,
            example = "null"
    )
    private LocalDateTime paidAt;

    @Schema(
            description = "Additional bill remarks",
            example = "Due date extended by Admin"
    )
    private String remarks;

    @Schema(
            description = "Admin auth user ID who generated the bill",
            example = "4"
    )
    private Long generatedBy;

    @Schema(
            description = "Timestamp when the bill was created",
            example = "2026-08-06T23:47:59"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the bill was last updated",
            example = "2026-08-07T11:40:40"
    )
    private LocalDateTime updatedAt;
}