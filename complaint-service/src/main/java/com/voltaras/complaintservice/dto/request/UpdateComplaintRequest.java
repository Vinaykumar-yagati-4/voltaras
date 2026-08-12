package com.voltaras.complaintservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the complaint owner to edit complaint details while the
 * complaint is still {@code OPEN}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateComplaintRequest", description = "Edit complaint details while OPEN")
public class UpdateComplaintRequest {

    @NotBlank(message = "Subject is required")
    @Size(min = 10, max = 200, message = "Subject must be between 10 and 200 characters")
    @Schema(description = "Updated complaint subject", example = "Incorrect bill amount for July 2026")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    @Schema(
            description = "Updated complaint description",
            example = "My bill shows 350 units consumed but I only used about 200 units. Please review."
    )
    private String description;
}
