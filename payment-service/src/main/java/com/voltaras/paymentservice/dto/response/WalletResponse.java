package com.voltaras.paymentservice.dto.response;

import com.voltaras.paymentservice.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Current wallet state of the authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "WalletResponse",
        description = "Wallet balance of the authenticated user"
)
public class WalletResponse {

    @Schema(description = "Wallet ID", example = "1")
    private Long id;

    @Schema(description = "Owner auth user ID", example = "13")
    private Long userId;

    @Schema(description = "Current available balance in INR", example = "1500.00")
    private BigDecimal balance;

    @Schema(description = "Wallet currency", example = "INR")
    private Currency currency;

    @Schema(
            description = "Timestamp when the wallet was created",
            example = "2026-08-08T09:00:00"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the wallet was last updated",
            example = "2026-08-08T10:16:01"
    )
    private LocalDateTime updatedAt;
}
