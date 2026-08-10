package com.voltaras.notificationservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for creating a manual notification (ADMIN only).
 *
 * <p>
 * Manual notifications always get type {@code MANUAL} and channel
 * {@code IN_APP}; those values are assigned by the service layer.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CreateNotificationRequest",
        description = "Payload for creating a manual notification as an ADMIN"
)
public class CreateNotificationRequest {

    @NotNull(message = "authUserId is required")
    @Positive(message = "authUserId must be a positive number")
    @Schema(description = "Auth Service user ID of the recipient", example = "13")
    private Long authUserId;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    @Schema(description = "Short notification title", example = "Voltage Maintenance")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message must not exceed 2000 characters")
    @Schema(
            description = "Notification body shown to the user",
            example = "Scheduled maintenance will cause a power cut on Sunday from 10:00 to 12:00."
    )
    private String message;

    @Size(max = 50, message = "referenceType must not exceed 50 characters")
    @Schema(
            description = "Optional business domain the notification refers to",
            example = "COMPLAINT",
            nullable = true
    )
    private String referenceType;

    @Schema(
            description = "Optional ID of the referenced business resource",
            example = "42",
            nullable = true
    )
    private Long referenceId;
}
