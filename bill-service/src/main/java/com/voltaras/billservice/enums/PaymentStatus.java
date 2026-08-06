package com.voltaras.billservice.enums;

/**
 * Payment state of a bill. Kept independent from {@link BillStatus} so a
 * bill can be PAID even while its payment record is, for example, FAILED,
 * and to prepare for the future Payment Service integration.
 */
public enum PaymentStatus {

    /** No payment has been received. Initial status for every bill. */
    UNPAID,

    /** Full amount has been received. */
    PAID,

    /** Only part of the total amount has been received. */
    PARTIALLY_PAID,

    /** The last payment attempt failed. */
    FAILED,

    /** A previous payment was refunded. */
    REFUNDED
}
