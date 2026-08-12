package com.voltaras.complaintservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for adding a comment to a complaint (consumer or admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AddComplaintCommentRequest", description = "Add a comment to a complaint")
public class AddComplaintCommentRequest {

    @NotBlank(message = "Comment text is required")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    @Schema(
            description = "Comment text",
            example = "The meter reading was corrected; the July bill has been regenerated."
    )
    private String commentText;
}
