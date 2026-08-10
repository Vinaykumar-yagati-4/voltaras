package com.voltaras.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Published by the Payment Service when a bill payment completes
 * successfully.
 *
 * <p>
 * Consumed by {@code PaymentCompletedNotificationListener} on the
 * {@code voltaras.payment.success.queue} queue; the listener converts it
 * into a {@code PAYMENT_SUCCESS} notification.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    /** ID of the payment transaction (Payment Service database). */
    private Long paymentId;

    /** ID of the bill that was paid (Bill Service database). */
    private Long billId;

    /** Auth Service user ID of the payer. */
    private Long authUserId;

    /** Amount paid in INR. */
    private BigDecimal amount;
}
