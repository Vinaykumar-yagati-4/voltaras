package com.voltaras.paymentservice.dto.request;

import com.voltaras.paymentservice.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for paying a bill from the VOLTARAS wallet balance. The wallet is
 * debited and the Bill Service is notified of the new payment status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "PayBillRequest",
        description = "Payload used to pay a bill from the wallet balance"
)
public class PayBillRequest {

    @NotNull
    @Positive
    @Schema(
            description = "Amount to pay from the wallet in INR. Must be positive and at most the outstanding amount.",
            example = "223.13"
    )
    private BigDecimal amount;

    @NotNull
    @Schema(
            description = "Currency. Only INR is supported.",
            example = "INR"
    )
    private Currency currency;

    @NotNull
    @Positive
    @Schema(
            description = "Organization the payer belongs to; active membership is validated by the service.",
            example = "6"
    )
    private Long organizationId;
}
