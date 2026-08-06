package com.voltaras.billservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used by administrators to cancel a bill.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CancelBillRequest",
        description = "Payload used by administrators to cancel a bill"
)
public class CancelBillRequest {

    @NotBlank
    @Schema(
            description = "Reason for cancelling the bill",
            example = "Meter reading was incorrect"
    )
    private String reason;
}
