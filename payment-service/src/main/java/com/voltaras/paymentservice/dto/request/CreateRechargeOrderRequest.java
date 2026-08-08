package com.voltaras.paymentservice.dto.request;

import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for creating a wallet recharge order at the Razorpay gateway.
 *
 * <p>
 * Only safe fields are accepted. No card numbers, CVVs, UPI PINs or
 * provider transaction IDs are ever accepted from the client.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CreateRechargeOrderRequest",
        description = "Payload used to create a wallet recharge order (UPI or CARD via Razorpay sandbox)"
)
public class CreateRechargeOrderRequest {

    @NotNull
    @Positive
    @Schema(
            description = "Recharge amount in INR. Must be positive.",
            example = "500.00"
    )
    private BigDecimal amount;

    @NotNull
    @Schema(
            description = "Currency. Only INR is supported.",
            example = "INR"
    )
    private Currency currency;

    @NotNull
    @Schema(
            description = "Payment method. Only UPI or CARD are accepted for recharges.",
            example = "UPI"
    )
    private PaymentMethod paymentMethod;

    @NotNull
    @Positive
    @Schema(
            description = "Organization the user belongs to; active membership is validated by the service.",
            example = "6"
    )
    private Long organizationId;

    @Size(max = 100)
    @Schema(
            description = "Optional client order note stored with the gateway order",
            example = "Voltaras wallet recharge",
            nullable = true
    )
    private String note;
}
