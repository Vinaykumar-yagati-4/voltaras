package com.voltaras.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentProvider;
import com.voltaras.paymentservice.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stable recharge transaction representation returned to callers. Never
 * exposes the JPA entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "RechargeTransactionResponse",
        description = "Wallet recharge transaction details"
)
public class RechargeTransactionResponse {

    @Schema(description = "Recharge transaction ID", example = "1")
    private Long id;

    @Schema(
            description = "Server-generated recharge reference",
            example = "RCH-3F9A2C7D..."
    )
    private String rechargeReference;

    @Schema(description = "Razorpay order ID", example = "order_xxxxxxxxxxxx")
    private String orderId;

    @Schema(description = "Recharge amount in INR", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private Currency currency;

    @Schema(description = "Payment method", example = "UPI")
    private PaymentMethod paymentMethod;

    @Schema(description = "Recharge status", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "Payment gateway provider", example = "RAZORPAY")
    private PaymentProvider provider;

    @Schema(
            description = "Razorpay payment ID received from the webhook",
            example = "pay_xxxxxxxxxxxx",
            nullable = true
    )
    private String providerTransactionId;

    @Schema(
            description = "Failure code when the recharge failed",
            example = "RAZORPAY_PAYMENT_FAILED",
            nullable = true
    )
    private String failureCode;

    @Schema(
            description = "Sanitized failure reason when the recharge failed",
            example = "Razorpay reported the payment as failed",
            nullable = true
    )
    private String failureReason;

    @Schema(
            description = "Timestamp when the recharge was created",
            example = "2026-08-08T10:15:30"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the recharge completed successfully",
            example = "2026-08-08T10:16:01",
            nullable = true
    )
    private LocalDateTime paidAt;
}
