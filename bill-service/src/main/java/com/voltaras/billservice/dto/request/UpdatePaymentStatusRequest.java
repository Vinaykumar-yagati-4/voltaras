package com.voltaras.billservice.dto.request;

import com.voltaras.billservice.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload used by administrators to update the payment state of a bill.
 *
 * <p>
 * This endpoint prepares the service for the future Payment Service
 * integration: payment reconciliation will update the same fields.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UpdatePaymentStatusRequest",
        description = "Payload used by administrators to update bill payment state"
)
public class UpdatePaymentStatusRequest {

    @Schema(
            description = "New payment status. PAID requires amountPaid >= totalAmount",
            example = "PAID"
    )
    private PaymentStatus paymentStatus;

    @PositiveOrZero
    @Schema(
            description = "Amount paid. Omitted when marking PAID means full payment",
            example = "262.50"
    )
    private BigDecimal amountPaid;

    @Schema(
            description = "When the payment was received. Defaults to now",
            example = "2026-06-10T14:30:00"
    )
    private LocalDateTime paidAt;

    @Schema(description = "Optional remarks about the payment", example = "UPI payment received")
    private String remarks;
}
