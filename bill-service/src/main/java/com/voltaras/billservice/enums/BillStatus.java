package com.voltaras.billservice.enums;

/**
 * Lifecycle status of an electricity bill.
 *
 * <p>
 * Initial status is {@link #GENERATED}. A bill whose due date has passed
 * without full payment is moved to {@link #OVERDUE}. {@link #CANCELLED}
 * bills are final and can never be paid.
 * </p>
 */
public enum BillStatus {

    /** Bill has been generated and issued to the consumer. */
    GENERATED,

    /** Bill is awaiting payment processing. */
    PENDING,

    /** Bill has been fully paid. */
    PAID,

    /** Due date has passed and the bill is not fully paid. */
    OVERDUE,

    /** Bill has been cancelled by an administrator. */
    CANCELLED
}
