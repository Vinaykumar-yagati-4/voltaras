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
 * Response returned after a recharge order is created. The Razorpay order
 * ID and key ID are the values the frontend needs to run the Razorpay
 * checkout in sandbox mode.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "RechargeOrderResponse",
        description = "Wallet recharge order created at the Razorpay gateway"
)
public class RechargeOrderResponse {

    @Schema(description = "Recharge transaction ID", example = "1")
    private Long id;

    @Schema(
            description = "Server-generated recharge reference",
            example = "RCH-3F9A2C7D..."
    )
    private String rechargeReference;

    @Schema(
            description = "Razorpay order ID; pass it to the Razorpay checkout (sandbox)",
            example = "order_xxxxxxxxxxxx"
    )
    private String orderId;

    @Schema(description = "Recharge amount in INR", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private Currency currency;

    @Schema(description = "Payment method used to fund the recharge", example = "UPI")
    private PaymentMethod paymentMethod;

    @Schema(description = "Recharge status", example = "CREATED")
    private PaymentStatus status;

    @Schema(description = "Payment gateway provider", example = "RAZORPAY")
    private PaymentProvider provider;

    @Schema(
            description = "Public Razorpay key ID used by the frontend checkout (not a secret)",
            example = "rzp_test_xxxxxxxx"
    )
    private String razorpayKeyId;

    @Schema(
            description = "Timestamp when the recharge order was created",
            example = "2026-08-08T10:15:30"
    )
    private LocalDateTime createdAt;
}
