package com.voltaras.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Published by the Payment Service when a wallet recharge completes
 * successfully.
 *
 * <p>
 * Consumed by {@code RechargeSuccessfulNotificationListener} on the
 * {@code voltaras.recharge.success.queue} queue; the listener converts it
 * into a {@code RECHARGE_SUCCESS} notification.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeSuccessfulEvent {

    /** ID of the recharge transaction (Payment Service database). */
    private Long rechargeTransactionId;

    /** Auth Service user ID of the wallet owner. */
    private Long authUserId;

    /** Recharged amount in INR. */
    private BigDecimal amount;
}
