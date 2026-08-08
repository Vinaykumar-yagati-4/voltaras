package com.voltaras.paymentservice.enums;

/**
 * Supported payment currencies.
 *
 * <p>
 * VOLTARAS bills are denominated in Indian Rupees (&#8377;), so {@link #INR}
 * is currently the only supported currency. Enum deserialization rejects
 * any other value with the MALFORMED_REQUEST error code.
 * </p>
 */
public enum Currency {

    /** Indian Rupee. */
    INR
}
