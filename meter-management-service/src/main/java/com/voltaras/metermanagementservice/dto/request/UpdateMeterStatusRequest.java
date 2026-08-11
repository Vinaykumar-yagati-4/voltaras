package com.voltaras.metermanagementservice.dto.request;

import com.voltaras.metermanagementservice.enums.MeterStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for updating the status of a meter (ADMIN only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UpdateMeterStatusRequest",
        description = "Payload for updating a meter status"
)
public class UpdateMeterStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New meter status", example = "FAULTY")
    private MeterStatus status;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Schema(description = "Optional remarks about the status change", example = "Display not working")
    private String remarks;
}
