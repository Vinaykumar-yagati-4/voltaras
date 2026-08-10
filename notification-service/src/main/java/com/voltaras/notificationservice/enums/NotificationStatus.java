package com.voltaras.notificationservice.enums;

/**
 * Read state of a notification.
 *
 * <ul>
 *     <li>{@link #UNREAD} &rarr; {@link #READ} (via PATCH read APIs)</li>
 *     <li>{@link #FAILED} &rarr; the notification could not be delivered
 *         (reserved for future EMAIL/SMS delivery)</li>
 * </ul>
 */
public enum NotificationStatus {

    /** Notification has not been opened yet. */
    UNREAD,

    /** Notification has been opened by the user. */
    READ,

    /** Delivery failed (reserved for EMAIL/SMS channels). */
    FAILED
}
