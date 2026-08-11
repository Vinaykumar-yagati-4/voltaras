package com.voltaras.metermanagementservice.enums;

/**
 * Lifecycle status of a physical electricity meter.
 */
public enum MeterStatus {

    /** Meter is installed and in service. */
    ACTIVE,

    /** Meter is installed but currently not in service. */
    INACTIVE,

    /** Meter is installed but malfunctioning and awaiting repair/replacement. */
    FAULTY,

    /** Meter has been replaced by another meter. */
    REPLACED,

    /** Meter has been removed from service (soft delete). */
    REMOVED
}
