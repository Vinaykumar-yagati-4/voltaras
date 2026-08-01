package com.voltaras.meterreadingservice.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeterReadingRequest {

    @NotNull(message = "Previous reading is required")
    @PositiveOrZero(message = "Previous reading must be zero or positive")
    @Digits(
            integer = 12,
            fraction = 3,
            message = "Previous reading has too many digits"
    )
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @PositiveOrZero(message = "Current reading must be zero or positive")
    @Digits(
            integer = 12,
            fraction = 3,
            message = "Current reading has too many digits"
    )
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    private LocalDate readingDate;

    @Size(
            max = 500,
            message = "Remarks must not exceed 500 characters"
    )
    private String remarks;
}