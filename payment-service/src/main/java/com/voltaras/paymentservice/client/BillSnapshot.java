package com.voltaras.paymentservice.client;

import java.math.BigDecimal;

/**
 * Immutable snapshot of bill data needed by the Payment Service. Statuses
 * are plain strings so the Payment Service is not coupled to Bill Service
 * enum classes.
 *
 * @param id bill ID
 * @param authUserId owning consumer auth user ID
 * @param billStatus bill lifecycle status (for example GENERATED, PAID, CANCELLED)
 * @param paymentStatus bill payment status (for example UNPAID, PARTIALLY_PAID, PAID)
 * @param totalAmount total payable amount
 * @param amountPaid cumulative amount already paid towards the bill
 * @param outstandingAmount remaining amount to pay
 */
public record BillSnapshot(
        Long id,
        Long authUserId,
        String billStatus,
        String paymentStatus,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingAmount) {
}
