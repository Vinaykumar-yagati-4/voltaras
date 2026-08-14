package com.voltaras.meterreadingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API response representing one day of the daily usage series.
 *
 * <p>When no reading was recorded for a date, {@code units} is zero and the
 * reading-related fields ({@code previousReading}, {@code currentReading},
 * {@code readingAt}) are {@code null} — the UI renders this as "no reading
 * recorded" rather than inventing values.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyUsageEntry {

    /** Calendar date the entry refers to. */
    private LocalDate date;

    /** Units consumed on this date (0 when no reading was recorded). */
    private BigDecimal units;

    /** Estimated cost of the day's units using the blended per-unit rate. */
    private BigDecimal estimatedCost;

    /** Meter reading value the day's consumption is measured from. */
    private BigDecimal previousReading;

    /** Meter reading value recorded on this date. */
    private BigDecimal currentReading;

    /** Timestamp of the recorded reading for this date. */
    private LocalDateTime readingAt;
}
