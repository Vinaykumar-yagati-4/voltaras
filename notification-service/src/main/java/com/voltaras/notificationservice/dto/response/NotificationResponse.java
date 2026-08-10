package com.voltaras.notificationservice.dto.response;

import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification details returned to the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "NotificationResponse",
        description = "A notification stored for a VOLTARAS user"
)
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Recipient auth user ID", example = "13")
    private Long authUserId;

    @Schema(description = "Notification title", example = "Bill Generated")
    private String title;

    @Schema(
            description = "Notification body",
            example = "Your electricity bill for August 2026 amounting to Rs. 1250.00 has been generated."
    )
    private String message;

    @Schema(description = "Notification type", example = "BILL_GENERATED")
    private NotificationType type;

    @Schema(description = "Delivery channel", example = "IN_APP")
    private NotificationChannel channel;

    @Schema(description = "Read state", example = "UNREAD")
    private NotificationStatus status;

    @Schema(
            description = "Business domain the notification refers to",
            example = "BILL",
            nullable = true
    )
    private String referenceType;

    @Schema(
            description = "ID of the referenced business resource",
            example = "12",
            nullable = true
    )
    private Long referenceId;

    @Schema(
            description = "Timestamp when the notification was read",
            example = "2026-08-08T10:16:01",
            nullable = true
    )
    private LocalDateTime readAt;

    @Schema(
            description = "Timestamp when the notification was created",
            example = "2026-08-08T09:00:00"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the notification was last updated",
            example = "2026-08-08T10:16:01"
    )
    private LocalDateTime updatedAt;
}
