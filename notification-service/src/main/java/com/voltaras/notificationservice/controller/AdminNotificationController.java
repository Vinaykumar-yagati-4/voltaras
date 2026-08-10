package com.voltaras.notificationservice.controller;

import com.voltaras.notificationservice.dto.request.CreateNotificationRequest;
import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADMIN-only notification APIs. The {@code X-User-Role} header must carry
 * {@code ADMIN} or {@code ROLE_ADMIN}, enforced in the service layer.
 */
@RestController
@RequestMapping("/api/notifications/admin")
@RequiredArgsConstructor
@Tag(
        name = "Admin Notification APIs",
        description = "ADMIN-only notification management (X-User-Role must be ADMIN)."
)
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(
            summary = "Create a manual notification",
            description = "Creates an IN_APP notification of type MANUAL for "
                    + "the given user. Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Manual notification created",
                    content = @Content(
                            schema = @Schema(implementation = NotificationResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role")
    })
    public ResponseEntity<NotificationResponse> createManualNotification(
            @Parameter(description = "Manual notification payload")
            @Valid @RequestBody CreateNotificationRequest request,
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        NotificationResponse response = notificationService
                .createManualNotification(adminUserId, systemRole, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{authUserId}")
    @Operation(
            summary = "Get notifications of a user",
            description = "Returns all notifications of the given user, "
                    + "newest first. Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role")
    })
    public ResponseEntity<List<NotificationResponse>> getUserNotificationsForAdmin(
            @Parameter(description = "Target user ID", example = "13")
            @PathVariable Long authUserId,
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "1")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        List<NotificationResponse> response = notificationService
                .getUserNotificationsForAdmin(adminUserId, systemRole, authUserId);

        return ResponseEntity.ok(response);
    }
}
