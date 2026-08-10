package com.voltaras.notificationservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Number of unread notifications of the authenticated user, used by the UI
 * to render the notification badge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UnreadNotificationCountResponse",
        description = "Unread notification count of the authenticated user"
)
public class UnreadNotificationCountResponse {

    @Schema(description = "Auth user ID", example = "13")
    private Long authUserId;

    @Schema(description = "Number of unread notifications", example = "4")
    private long unreadCount;
}
