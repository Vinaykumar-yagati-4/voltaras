package com.voltaras.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentProvider;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stable bill-payment representation returned to callers. Never exposes
 * the JPA entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "PaymentResponse",
        description = "Bill payment settled from the wallet"
)
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "1")
    private Long id;

    @Schema(
            description = "Server-generated payment reference",
            example = "PAY-3F9A2C7D..."
    )
    private String paymentReference;

    @Schema(
            description = "Idempotency key supplied by the client",
            example = "pay-bill-1-2026-08-08"
    )
    private String idempotencyKey;

    @Schema(description = "Transaction type", example = "BILL_PAYMENT")
    private TransactionType transactionType;

    @Schema(description = "Bill ID being paid", example = "1")
    private Long billId;

    @Schema(description = "Organization ID of the payer", example = "6")
    private Long organizationId;

    @Schema(description = "Payer auth user ID", example = "13")
    private Long userId;

    @Schema(description = "Amount paid from the wallet", example = "223.13")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private Currency currency;

    @Schema(description = "Payment method (always WALLET for bill payments)", example = "WALLET")
    private PaymentMethod paymentMethod;

    @Schema(description = "Payment status", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(
            description = "Payment provider. Null for wallet-funded bill payments.",
            example = "RAZORPAY",
            nullable = true
    )
    private PaymentProvider provider;

    @Schema(
            description = "Provider-generated transaction ID (never client-supplied)",
            nullable = true
    )
    private String providerTransactionId;

    @Schema(
            description = "Failure code when the payment failed",
            nullable = true
    )
    private String failureCode;

    @Schema(
            description = "Sanitized failure reason when the payment failed",
            nullable = true
    )
    private String failureReason;

    @Schema(
            description = "Timestamp when the payment was created",
            example = "2026-08-08T11:40:40"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the payment was last updated",
            example = "2026-08-08T11:40:41"
    )
    private LocalDateTime updatedAt;

    @Schema(
            description = "Timestamp when the payment completed successfully",
            example = "2026-08-08T11:40:41",
            nullable = true
    )
    private LocalDateTime paidAt;
}
