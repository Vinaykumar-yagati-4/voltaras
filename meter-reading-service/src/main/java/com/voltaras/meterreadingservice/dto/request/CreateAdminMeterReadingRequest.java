package com.voltaras.meterreadingservice.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload used by system ADMINs to record a meter reading on behalf of a
 * consumer (account preparation). The consumer is identified by
 * {@code authUserId}; the authenticated Admin ID comes from the
 * X-User-Id header and is recorded for the audit trail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminMeterReadingRequest {

    @NotNull(message = "authUserId is required")
    @Positive(message = "authUserId must be positive")
    private Long authUserId;

    @NotBlank(message = "Meter number is required")
    @Size(
            max = 50,
            message = "Meter number must not exceed 50 characters"
    )
    private String meterNumber;

    @NotNull(message = "Previous reading is required")
    @PositiveOrZero(
            message = "Previous reading must be zero or positive"
    )
    @Digits(
            integer = 12,
            fraction = 3,
            message = "Previous reading has too many digits"
    )
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @PositiveOrZero(
            message = "Current reading must be zero or positive"
    )
    @Digits(
            integer = 12,
            fraction = 3,
            message = "Current reading has too many digits"
    )
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(
            message = "Reading date cannot be in the future"
    )
    private LocalDate readingDate;

    @Size(
            max = 500,
            message = "Remarks must not exceed 500 characters"
    )
    private String remarks;
}
