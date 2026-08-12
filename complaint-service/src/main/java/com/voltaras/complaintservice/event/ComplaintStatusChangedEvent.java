package com.voltaras.complaintservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published when the status of a complaint changes.
 *
 * <p>
 * Mirrors the envelope consumed by the Notification Service on the
 * {@code voltaras.complaint.status.queue} queue ({@code complaintId},
 * {@code authUserId}, {@code status}); the logical type ID
 * {@code ComplaintStatusChangedEvent} is written on the RabbitMQ
 * {@code __TypeId__} header so the Notification Service can deserialize it
 * into its own event class of the same name.
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
