package com.voltaras.notificationservice.enums;

/**
 * Delivery channel of a notification.
 *
 * <p>
 * All notifications are stored in the notification database regardless of
 * channel. Only {@link #IN_APP} delivery is implemented today; EMAIL and
 * SMS channels are part of the domain model so they can be activated later
 * without schema or API changes.
 * </p>
 */
public enum NotificationChannel {

    /** In-app notification visible inside the VOLTARAS platform. */
    IN_APP,

    /** Email delivery. */
    EMAIL,

    /** SMS delivery. */
    SMS
}
