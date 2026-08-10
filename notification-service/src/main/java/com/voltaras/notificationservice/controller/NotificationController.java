package com.voltaras.notificationservice.controller;

import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.voltaras.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notification APIs of the authenticated user.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(
        name = "Notification APIs",
        description = "In-app notifications of the authenticated user."
)
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "Get my notifications",
            description = "Returns all notifications of the authenticated "
                    + "user, newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications retrieved",
                    content = @Content(
                            schema = @Schema(implementation = NotificationResponse.class)
                    )
            )
    })
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(notificationService.getMyNotifications(authUserId));
    }

    @GetMapping("/unread")
    @Operation(
            summary = "Get my unread notifications",
            description = "Returns only the unread notifications of the "
                    + "authenticated user, newest first."
    )
    public ResponseEntity<List<NotificationResponse>> getMyUnreadNotifications(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(notificationService.getMyUnreadNotifications(authUserId));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Mark a notification as read",
            description = "Marks the notification as READ. The notification "
                    + "must belong to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found or not owned by the user")
    })
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(notificationService.markAsRead(authUserId, id));
    }

    @PatchMapping("/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks every unread notification of the "
                    + "authenticated user as READ."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All notifications marked as read")
    })
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        notificationService.markAllAsRead(authUserId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/unread")
    @Operation(
            summary = "Get my unread notification count",
            description = "Returns the number of unread notifications of the "
                    + "authenticated user, used by the UI notification badge."
    )
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        return ResponseEntity.ok(notificationService.getUnreadCount(authUserId));
    }
}
