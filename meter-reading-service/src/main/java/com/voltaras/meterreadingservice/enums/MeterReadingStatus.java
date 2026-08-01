package com.voltaras.meterreadingservice.enums;

/**
 * Lifecycle status of a meter reading.
 * <ul>
 *   <li>{@link #SUBMITTED} — created by the consumer, awaiting admin review.</li>
 *   <li>{@link #VERIFIED}  — approved by an ADMIN.</li>
 *   <li>{@link #REJECTED}  — rejected by an ADMIN (with remarks).</li>
 * </ul>
 */
public enum MeterReadingStatus {
    SUBMITTED,
    VERIFIED,
    REJECTED
}
