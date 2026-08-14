package com.voltaras.meterreadingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API response for the consumer daily electricity usage tracking endpoints.
 *
 * <p>All values are calculated by the backend from the consumer's real
 * recorded meter readings (SUBMITTED or VERIFIED; REJECTED readings are
 * excluded). Cost fields are estimates derived from the bill-service
 * tariff slabs and are explicitly labelled as such.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyUsageResponse {

    /** Meter number the usage series is tracked for. */
    private String meterNumber;

    /** Date the summary refers to (the backend's current date). */
    private LocalDate usageDate;

    /** Meter value recorded just before {@link #usageDate}. */
    private BigDecimal previousReading;

    /** Meter value of the latest recorded reading (today's when present). */
    private BigDecimal latestReading;

    /** Timestamp of the reading used for {@link #previousReading}. */
    private LocalDateTime previousReadingAt;

    /** Timestamp of the reading used for {@link #latestReading}. */
    private LocalDateTime latestReadingAt;

    /** Units consumed today (0 when no reading has been recorded today). */
    private BigDecimal unitsConsumedToday;

    /** Effective per-unit rate estimate (blended tariff rate). */
    private BigDecimal estimatedPerUnitCost;

    /** Estimated cost of today's consumption. */
    private BigDecimal estimatedTodayCost;

    /** Units consumed so far in the current billing month. */
    private BigDecimal monthUnitsSoFar;

    /** Estimated month-to-date bill (energy charge + fixed charge + tax). */
    private BigDecimal estimatedMonthCost;

    /** True when the consumer has at least one recorded (non-rejected) reading. */
    private boolean hasReadings;

    /** True when a reading has been recorded for {@link #usageDate}. */
    private boolean hasReadingToday;

    /** Daily usage series for the requested look-back window, oldest first. */
    private List<DailyUsageEntry> dailyUsage;
}
