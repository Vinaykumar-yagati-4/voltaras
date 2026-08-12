package com.voltaras.complaintservice.enums;

/**
 * The four complaint categories seeded at startup (documented in
 * {@code docs/02_REQUIREMENTS.md} FR-07.01 and {@code docs/04_DATABASE.md}).
 */
public enum ComplaintCategoryName {

    BILLING_ISSUE("Issues with the computed electricity bill"),

    METER_ISSUE("Issues with the electricity meter or its readings"),

    PAYMENT_ISSUE("Issues with payments or payment receipts"),

    OTHER("Any other complaint");

    private final String description;

    ComplaintCategoryName(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
