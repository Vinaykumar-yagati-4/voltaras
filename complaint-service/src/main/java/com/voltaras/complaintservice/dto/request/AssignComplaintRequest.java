package com.voltaras.complaintservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for assigning a complaint to an admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AssignComplaintRequest", description = "Assign a complaint to an admin")
public class AssignComplaintRequest {

    @NotNull(message = "Assignee user ID is required")
    @Positive(message = "Assignee user ID must be positive")
    @Schema(description = "Auth Service admin user ID to assign the complaint to", example = "2")
    private Long assignedTo;
}
