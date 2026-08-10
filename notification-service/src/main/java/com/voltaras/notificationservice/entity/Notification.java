package com.voltaras.notificationservice.entity;

import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A notification stored for a VOLTARAS user.
 *
 * <p>
 * Notifications are created either by the RabbitMQ listeners (event-driven,
 * one Notification per consumed event) or manually by an ADMIN through the
 * REST API. Only the Auth Service user ID is stored; no JPA relationship is
 * created to other service databases.
 * </p>
 *
 * <p>
 * {@code referenceType} / {@code referenceId} are the optional business
 * reference of the notification (for example type {@code BILL} and the bill
 * ID) so the UI can deep-link to the source resource.
 * </p>
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_auth_user_id", columnList = "auth_user_id"),
                @Index(name = "idx_notifications_auth_user_id_status", columnList = "auth_user_id, status"),
                @Index(name = "idx_notifications_status", columnList = "status"),
                @Index(name = "idx_notifications_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Auth Service user ID of the notification recipient, received from
     * X-User-Id (or from the event payload for event-driven notifications).
     */
    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    /**
     * Business domain the notification refers to (BILL, PAYMENT, RECHARGE,
     * COMPLAINT, ...).
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * ID of the referenced business resource.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
