package com.voltaras.metermanagementservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for assigning a meter to a consumer (ADMIN only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "AssignMeterRequest",
        description = "Payload for assigning a meter to a consumer"
)
public class AssignMeterRequest {

    @NotNull(message = "authUserId is required when assigning a meter")
    @Schema(description = "Consumer the meter is assigned to", example = "100")
    private Long authUserId;

    @Schema(description = "Optional organization the meter belongs to", example = "7")
    private Long organizationId;
}
