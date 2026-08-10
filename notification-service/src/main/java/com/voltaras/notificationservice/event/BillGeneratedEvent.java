package com.voltaras.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Published by the Bill Service when a new bill is generated.
 *
 * <p>
 * Consumed by {@code BillGeneratedNotificationListener} on the
 * {@code voltaras.bill.generated.queue} queue; the listener converts it into
 * a {@code BILL_GENERATED} notification.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillGeneratedEvent {

    /** ID of the generated bill (Bill Service database). */
    private Long billId;

    /** Auth Service user ID of the bill owner. */
    private Long authUserId;

    /** Total bill amount in INR. */
    private BigDecimal amount;

    /** Billing period the bill covers, e.g. "August 2026". */
    private String billingPeriod;
}
