package com.voltaras.complaintservice.enums;

/**
 * Priority of a VOLTARAS complaint. New complaints default to
 * {@link #NORMAL}; priority is not user-editable.
 */
public enum ComplaintPriority {

    LOW,

    /** Default priority for newly raised complaints. */
    NORMAL,

    HIGH,

    URGENT
}
