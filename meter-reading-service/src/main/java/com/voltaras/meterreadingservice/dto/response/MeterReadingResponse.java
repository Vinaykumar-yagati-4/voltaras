package com.voltaras.meterreadingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API response representing a meter reading.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeterReadingResponse {

    private Long id;

    private Long authUserId;

    private String meterNumber;

    private Integer billingMonth;

    private Integer billingYear;

    private BigDecimal previousReading;

    private BigDecimal currentReading;

    private BigDecimal unitsConsumed;

    private LocalDate readingDate;

    private MeterReadingStatus status;

    private String remarks;

    private Long verifiedBy;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
