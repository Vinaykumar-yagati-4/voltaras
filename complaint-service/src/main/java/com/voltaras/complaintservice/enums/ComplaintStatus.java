package com.voltaras.complaintservice.enums;

/**
 * Lifecycle status of a VOLTARAS complaint.
 *
 * <p>
 * Allowed transitions (enforced by
 * {@link com.voltaras.complaintservice.util.ComplaintStatusTransitions}):
 * {@code OPEN -&gt; IN_PROGRESS -&gt; RESOLVED -&gt; CLOSED}. {@code CLOSED}
 * is terminal; complaints are never cancelled or deleted.
 * </p>
 */
public enum ComplaintStatus {

    /** Complaint raised and awaiting an admin. */
    OPEN,

    /** An admin is working on the complaint. */
    IN_PROGRESS,

    /** The complaint has been resolved (resolution timestamp recorded). */
    RESOLVED,

    /** The complaint has been closed (terminal state). */
    CLOSED
}
