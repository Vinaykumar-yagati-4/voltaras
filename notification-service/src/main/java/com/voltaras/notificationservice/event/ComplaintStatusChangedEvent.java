package com.voltaras.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published when the status of a complaint changes.
 *
 * <p>
 * Consumed by {@code ComplaintStatusChangedNotificationListener} on the
 * {@code voltaras.complaint.status.queue} queue; the listener converts it
 * into a {@code COMPLAINT_STATUS_UPDATED} notification.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusChangedEvent {

    /** ID of the complaint. */
    private Long complaintId;

    /** Auth Service user ID of the complaint owner. */
    private Long authUserId;

    /** New complaint status, e.g. "IN_PROGRESS" or "RESOLVED". */
    private String status;
}
