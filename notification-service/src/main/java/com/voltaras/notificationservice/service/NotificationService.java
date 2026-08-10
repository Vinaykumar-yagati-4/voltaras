package com.voltaras.notificationservice.service;

import com.voltaras.notificationservice.dto.request.CreateNotificationRequest;
import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationType;

import java.util.List;

/**
 * Notification operations: event-driven creation, manual ADMIN creation and
 * the user-facing read/update APIs.
 */
public interface NotificationService {

    /**
     * Creates and persists a notification. Called by the RabbitMQ listeners
     * for every consumed domain event.
     *
     * @param authUserId recipient auth user ID
     * @param title notification title
     * @param message notification body
     * @param type notification type (BILL_GENERATED, PAYMENT_SUCCESS, ...)
     * @param channel delivery channel (IN_APP today)
     * @param referenceType optional business reference type (BILL, PAYMENT, ...)
     * @param referenceId optional business reference ID
     * @return the stored notification
     */
    NotificationResponse createNotification(
            Long authUserId,
            String title,
            String message,
            NotificationType type,
            NotificationChannel channel,
            String referenceType,
            Long referenceId);

    /**
     * Creates a manual notification as an ADMIN. The notification always
     * gets type {@code MANUAL} and channel {@code IN_APP}.
     *
     * @param adminUserId authenticated ADMIN user ID
     * @param systemRole role from X-User-Role
     * @param request manual notification payload
     * @return the stored notification
     */
    NotificationResponse createManualNotification(
            Long adminUserId, String systemRole, CreateNotificationRequest request);

    /**
     * Returns all notifications of the authenticated user, newest first.
     */
    List<NotificationResponse> getMyNotifications(Long authUserId);

    /**
     * Returns the unread notifications of the authenticated user, newest
     * first.
     */
    List<NotificationResponse> getMyUnreadNotifications(Long authUserId);

    /**
     * Marks a notification owned by the authenticated user as READ.
     *
     * @throws com.voltaras.notificationservice.exception.ResourceNotFoundException
     *         when the notification does not exist or belongs to another user
     */
    NotificationResponse markAsRead(Long authUserId, Long notificationId);

    /**
     * Marks every unread notification of the authenticated user as READ.
     */
    void markAllAsRead(Long authUserId);

    /**
     * Returns the number of unread notifications of the authenticated user.
     */
    UnreadNotificationCountResponse getUnreadCount(Long authUserId);

    /**
     * Returns all notifications of the target user, newest first. ADMIN only.
     *
     * @param adminUserId authenticated ADMIN user ID
     * @param systemRole role from X-User-Role
     * @param targetAuthUserId user whose notifications are requested
     * @return notifications of the target user
     */
    List<NotificationResponse> getUserNotificationsForAdmin(
            Long adminUserId, String systemRole, Long targetAuthUserId);
}
