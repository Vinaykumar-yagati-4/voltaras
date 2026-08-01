package com.voltaras.meterreadingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for rejecting a meter reading.
 * <p>
 * Remarks are mandatory — an admin must explain why the reading
 * was rejected.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectMeterReadingRequest {

    @NotBlank(message = "Remarks are required for rejection")
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
