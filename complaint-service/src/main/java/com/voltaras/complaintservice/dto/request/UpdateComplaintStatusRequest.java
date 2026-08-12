package com.voltaras.complaintservice.dto.request;

import com.voltaras.complaintservice.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the admin status-transition operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateComplaintStatusRequest", description = "Move a complaint to a new lifecycle status")
public class UpdateComplaintStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(
            description = "Target status. Allowed transitions: "
                    + "OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED",
            example = "IN_PROGRESS"
    )
    private ComplaintStatus status;
}
