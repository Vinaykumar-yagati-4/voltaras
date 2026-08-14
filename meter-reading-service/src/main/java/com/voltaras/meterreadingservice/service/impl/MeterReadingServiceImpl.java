package com.voltaras.meterreadingservice.service.impl;

import com.voltaras.meterreadingservice.dto.request.RejectMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.UpdateMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.response.DailyUsageEntry;
import com.voltaras.meterreadingservice.dto.response.DailyUsageResponse;
import com.voltaras.meterreadingservice.dto.response.MeterReadingResponse;
import com.voltaras.meterreadingservice.entity.MeterReading;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import com.voltaras.meterreadingservice.exception.BadRequestException;
import com.voltaras.meterreadingservice.exception.DuplicateResourceException;
import com.voltaras.meterreadingservice.exception.ForbiddenOperationException;
import com.voltaras.meterreadingservice.exception.ResourceNotFoundException;
import com.voltaras.meterreadingservice.mapper.MeterReadingMapper;
import com.voltaras.meterreadingservice.repository.MeterReadingRepository;
import com.voltaras.meterreadingservice.service.MeterReadingService;
import com.voltaras.meterreadingservice.util.TariffCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implements the meter-reading business rules.
 *
 * Daily readings are stored using readingDate.
 * Billing month and billing year are derived automatically
 * from readingDate for monthly and yearly reporting.
 */
@Service
@RequiredArgsConstructor
public class MeterReadingServiceImpl implements MeterReadingService {

    private static final Logger log =
            LoggerFactory.getLogger(MeterReadingServiceImpl.class);

    private static final String ADMIN_ROLE = "ADMIN";

    /** Default look-back window for the daily usage series. */
    private static final int DEFAULT_WINDOW_DAYS = 7;

    /** Smallest allowed look-back window. */
    private static final int MIN_WINDOW_DAYS = 1;

    /** Largest allowed look-back window. */
    private static final int MAX_WINDOW_DAYS = 31;

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final MeterReadingRepository meterReadingRepository;

    /**
     * Clock used to determine "today". Overridable through
     * {@link #setClock(Clock)} so tests can assert deterministic dates.
     */
    private Clock clock = Clock.systemDefaultZone();

    /** Test seam — replaces the clock used for "today". */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    // ==============================================================
    // Consumer operations
    // ==============================================================

    @Override
    @Transactional
    public MeterReadingResponse submitReading(
            Long authUserId,
            SubmitMeterReadingRequest request
    ) {

        validateReadingValues(
                request.getPreviousReading(),
                request.getCurrentReading()
        );

        // One reading per consumer, meter and date.
        if (meterReadingRepository
                .existsByAuthUserIdAndMeterNumberAndReadingDate(
                        authUserId,
                        request.getMeterNumber(),
                        request.getReadingDate()
                )) {

            log.warn(
                    "Duplicate meter reading rejected: " +
                            "authUserId={}, meterNumber={}, readingDate={}",
                    authUserId,
                    request.getMeterNumber(),
                    request.getReadingDate()
            );

            throw new DuplicateResourceException(
                    "MeterReading",
                    "meterNumber, readingDate",
                    request.getMeterNumber()
                            + ", "
                            + request.getReadingDate()
            );
        }

        MeterReading reading =
                MeterReadingMapper.toEntity(request);

        // Authenticated identity comes only from API Gateway.
        reading.setAuthUserId(authUserId);

        // Month and year are derived from readingDate.
        reading.setBillingMonth(
                request.getReadingDate().getMonthValue()
        );

        reading.setBillingYear(
                request.getReadingDate().getYear()
        );

        // System-controlled calculated field.
        reading.setUnitsConsumed(
                calculateUnitsConsumed(
                        request.getPreviousReading(),
                        request.getCurrentReading()
                )
        );

        // New readings always begin with SUBMITTED status.
        reading.setStatus(MeterReadingStatus.SUBMITTED);

        MeterReading savedReading =
                meterReadingRepository.save(reading);

        log.info(
                "Meter reading submitted: readingId={}, authUserId={}, " +
                        "meterNumber={}, unitsConsumed={}, readingDate={}, " +
                        "billingMonth={}, billingYear={}",
                savedReading.getId(),
                savedReading.getAuthUserId(),
                savedReading.getMeterNumber(),
                savedReading.getUnitsConsumed(),
                savedReading.getReadingDate(),
                savedReading.getBillingMonth(),
                savedReading.getBillingYear()
        );

        return MeterReadingMapper.toResponse(savedReading);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getMyReadings(
            Long authUserId
    ) {

        return meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(authUserId)
                .stream()
                .map(MeterReadingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeterReadingResponse getMyReadingById(
            Long authUserId,
            Long readingId
    ) {

        MeterReading reading =
                findOwnedReading(authUserId, readingId);

        return MeterReadingMapper.toResponse(reading);
    }

    @Override
    @Transactional
    public MeterReadingResponse updateMyReading(
            Long authUserId,
            Long readingId,
            UpdateMeterReadingRequest request
    ) {

        MeterReading reading =
                findOwnedReading(authUserId, readingId);

        ensureEditable(reading);

        validateReadingValues(
                request.getPreviousReading(),
                request.getCurrentReading()
        );

        /*
         * If readingDate can be changed during update,
         * check whether another reading already exists
         * for the same meter and new date.
         */
        if (request.getReadingDate() != null) {

            boolean duplicateExists =
                    meterReadingRepository
                            .existsByAuthUserIdAndMeterNumberAndReadingDateAndIdNot(
                                    authUserId,
                                    reading.getMeterNumber(),
                                    request.getReadingDate(),
                                    readingId
                            );

            if (duplicateExists) {

                throw new DuplicateResourceException(
                        "MeterReading",
                        "meterNumber, readingDate",
                        reading.getMeterNumber()
                                + ", "
                                + request.getReadingDate()
                );
            }
        }

        MeterReadingMapper.updateEntity(reading, request);

        // Recalculate month and year after date update.
        if (reading.getReadingDate() != null) {

            reading.setBillingMonth(
                    reading.getReadingDate().getMonthValue()
            );

            reading.setBillingYear(
                    reading.getReadingDate().getYear()
            );
        }

        reading.setUnitsConsumed(
                calculateUnitsConsumed(
                        reading.getPreviousReading(),
                        reading.getCurrentReading()
                )
        );

        MeterReading updatedReading =
                meterReadingRepository.save(reading);

        log.info(
                "Meter reading updated: readingId={}, authUserId={}, " +
                        "readingDate={}, unitsConsumed={}",
                updatedReading.getId(),
                updatedReading.getAuthUserId(),
                updatedReading.getReadingDate(),
                updatedReading.getUnitsConsumed()
        );

        return MeterReadingMapper.toResponse(updatedReading);
    }

    @Override
    @Transactional
    public void deleteMyReading(
            Long authUserId,
            Long readingId
    ) {

        MeterReading reading =
                findOwnedReading(authUserId, readingId);

        ensureEditable(reading);

        meterReadingRepository.delete(reading);

        log.info(
                "Meter reading deleted: readingId={}, authUserId={}",
                readingId,
                authUserId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DailyUsageResponse getDailyUsage(Long authUserId) {
        return buildDailyUsage(authUserId, DEFAULT_WINDOW_DAYS);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyUsageResponse getUsageSummary(Long authUserId, int days) {
        int window = Math.max(
                MIN_WINDOW_DAYS,
                Math.min(days, MAX_WINDOW_DAYS)
        );
        return buildDailyUsage(authUserId, window);
    }

    /**
     * Builds the daily usage summary from the consumer's real recorded
     * readings (SUBMITTED and VERIFIED; REJECTED readings are excluded).
     *
     * <p>Usage is tracked for the consumer's most recently active meter.
     * Daily consumption is the difference between consecutive recorded
     * meter values; a day without a recorded reading reports zero units
     * instead of a fabricated value. Cost fields are estimates using the
     * bill-service tariff slabs.</p>
     */
    private DailyUsageResponse buildDailyUsage(
            Long authUserId,
            int windowDays
    ) {
        LocalDate today = LocalDate.now(clock);

        List<MeterReading> readings = meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(authUserId)
                .stream()
                .filter(r -> r.getStatus() != MeterReadingStatus.REJECTED)
                .sorted(Comparator
                        .comparing(MeterReading::getReadingDate)
                        .thenComparing(
                                r -> r.getCreatedAt() == null
                                        ? LocalDateTime.MIN
                                        : r.getCreatedAt()
                        )
                        .thenComparing(MeterReading::getId))
                .toList();

        if (readings.isEmpty()) {
            return DailyUsageResponse.builder()
                    .usageDate(today)
                    .unitsConsumedToday(ZERO)
                    .estimatedPerUnitCost(TariffCalculator.RATE_SLAB_ONE)
                    .estimatedTodayCost(ZERO)
                    .monthUnitsSoFar(ZERO)
                    .estimatedMonthCost(ZERO)
                    .hasReadings(false)
                    .hasReadingToday(false)
                    .dailyUsage(List.of())
                    .build();
        }

        // Track the most recently active meter only, so readings from a
        // second meter never double-count consumption.
        MeterReading latest = readings.get(readings.size() - 1);
        String meterNumber = latest.getMeterNumber();
        List<MeterReading> meterReadings = readings.stream()
                .filter(r -> meterNumber.equals(r.getMeterNumber()))
                .toList();

        // ----- Current month (needed for the blended rate used by day costs)
        YearMonth currentMonth = YearMonth.from(today);
        List<MeterReading> monthReadings = meterReadings.stream()
                .filter(r -> YearMonth.from(r.getReadingDate())
                        .equals(currentMonth))
                .toList();

        MeterReading lastInMonth = monthReadings.isEmpty()
                ? null
                : monthReadings.get(monthReadings.size() - 1);

        MeterReading beforeMonth = null;
        for (MeterReading r : meterReadings) {
            if (r.getReadingDate().isBefore(currentMonth.atDay(1))) {
                beforeMonth = r;
            } else {
                break;
            }
        }

        BigDecimal monthUnits;
        if (lastInMonth == null) {
            monthUnits = ZERO;
        } else {
            BigDecimal base = beforeMonth != null
                    ? beforeMonth.getCurrentReading()
                    : lastInMonth.getPreviousReading();
            monthUnits = lastInMonth.getCurrentReading().subtract(base);
            if (monthUnits.signum() < 0) {
                monthUnits = ZERO;
            }
        }

        BigDecimal perUnit = TariffCalculator.blendedPerUnitRate(monthUnits);

        // ----- Daily series ----------------------------------------------
        LocalDate windowStart = today.minusDays(windowDays - 1L);

        // Baseline before the window: latest recorded meter value strictly
        // before the window start.
        MeterReading beforeWindow = null;
        for (MeterReading r : meterReadings) {
            if (r.getReadingDate().isBefore(windowStart)) {
                beforeWindow = r;
            } else {
                break;
            }
        }

        // Latest reading per date within the window (a consumer can record
        // at most one reading per meter per date).
        Map<LocalDate, MeterReading> byDate = new TreeMap<>();
        for (MeterReading r : meterReadings) {
            if (!r.getReadingDate().isBefore(windowStart)
                    && !r.getReadingDate().isAfter(today)) {
                byDate.put(r.getReadingDate(), r);
            }
        }

        List<DailyUsageEntry> dailyUsage = new ArrayList<>();
        BigDecimal running = beforeWindow != null
                ? beforeWindow.getCurrentReading()
                : null;

        for (LocalDate d = windowStart;
                !d.isAfter(today);
                d = d.plusDays(1)) {

            MeterReading reading = byDate.get(d);
            if (reading == null) {
                dailyUsage.add(DailyUsageEntry.builder()
                        .date(d)
                        .units(ZERO)
                        .build());
                continue;
            }

            BigDecimal base = running != null
                    ? running
                    : reading.getPreviousReading();
            BigDecimal units = reading.getCurrentReading()
                    .subtract(base);
            if (units.signum() < 0) {
                units = ZERO;
            }

            dailyUsage.add(DailyUsageEntry.builder()
                    .date(d)
                    .units(units)
                    .estimatedCost(scaleMoney(units.multiply(perUnit)))
                    .previousReading(base)
                    .currentReading(reading.getCurrentReading())
                    .readingAt(reading.getCreatedAt())
                    .build());

            running = reading.getCurrentReading();
        }

        // ----- Today -----------------------------------------------------
        MeterReading todayReading = byDate.get(today);
        boolean hasReadingToday = todayReading != null;

        MeterReading beforeToday = null;
        for (MeterReading r : meterReadings) {
            if (r.getReadingDate().isBefore(today)) {
                beforeToday = r;
            } else {
                break;
            }
        }

        BigDecimal previousReading;
        LocalDateTime previousReadingAt;
        BigDecimal unitsToday;

        if (hasReadingToday) {
            BigDecimal base = beforeToday != null
                    ? beforeToday.getCurrentReading()
                    : todayReading.getPreviousReading();
            unitsToday = todayReading.getCurrentReading().subtract(base);
            if (unitsToday.signum() < 0) {
                unitsToday = ZERO;
            }
            previousReading = base;
            previousReadingAt = beforeToday != null
                    ? beforeToday.getCreatedAt()
                    : null;
        } else {
            unitsToday = ZERO;
            previousReading = beforeToday != null
                    ? beforeToday.getCurrentReading()
                    : null;
            previousReadingAt = beforeToday != null
                    ? beforeToday.getCreatedAt()
                    : null;
        }

        BigDecimal latestReading = hasReadingToday
                ? todayReading.getCurrentReading()
                : latest.getCurrentReading();
        LocalDateTime latestReadingAt = hasReadingToday
                ? todayReading.getCreatedAt()
                : latest.getCreatedAt();

        BigDecimal todayCost = scaleMoney(unitsToday.multiply(perUnit));

        return DailyUsageResponse.builder()
                .meterNumber(meterNumber)
                .usageDate(today)
                .previousReading(previousReading)
                .latestReading(latestReading)
                .previousReadingAt(previousReadingAt)
                .latestReadingAt(latestReadingAt)
                .unitsConsumedToday(scaleMoney(unitsToday))
                .estimatedPerUnitCost(perUnit)
                .estimatedTodayCost(todayCost)
                .monthUnitsSoFar(scaleMoney(monthUnits))
                .estimatedMonthCost(
                        TariffCalculator.estimatedMonthCost(monthUnits)
                )
                .hasReadings(true)
                .hasReadingToday(hasReadingToday)
                .dailyUsage(dailyUsage)
                .build();
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(
                TariffCalculator.MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    // ==============================================================
    // Admin operations
    // ==============================================================

    @Override
    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getAllReadingsForAdmin(
            String role,
            MeterReadingStatus status
    ) {

        requireAdminRole(role);

        List<MeterReading> readings;

        if (status == null) {

            readings =
                    meterReadingRepository
                            .findAllByOrderByCreatedAtDesc();

        } else {

            readings =
                    meterReadingRepository
                            .findAllByStatusOrderByCreatedAtDesc(status);
        }

        return readings.stream()
                .map(MeterReadingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MeterReadingResponse verifyReading(
            Long adminUserId,
            String role,
            Long readingId
    ) {

        requireAdminRole(role);

        MeterReading reading =
                findReadingById(readingId);

        reading.setStatus(MeterReadingStatus.VERIFIED);
        reading.setVerifiedBy(adminUserId);
        reading.setVerifiedAt(LocalDateTime.now());

        MeterReading verifiedReading =
                meterReadingRepository.save(reading);

        log.info(
                "Meter reading verified: readingId={}, verifiedBy={}",
                verifiedReading.getId(),
                verifiedReading.getVerifiedBy()
        );

        return MeterReadingMapper.toResponse(verifiedReading);
    }

    @Override
    @Transactional
    public MeterReadingResponse rejectReading(
            Long adminUserId,
            String role,
            Long readingId,
            RejectMeterReadingRequest request
    ) {

        requireAdminRole(role);

        MeterReading reading =
                findReadingById(readingId);

        reading.setStatus(MeterReadingStatus.REJECTED);
        reading.setRemarks(request.getRemarks());
        reading.setVerifiedBy(adminUserId);
        reading.setVerifiedAt(LocalDateTime.now());

        MeterReading rejectedReading =
                meterReadingRepository.save(reading);

        log.info(
                "Meter reading rejected: readingId={}, rejectedBy={}",
                rejectedReading.getId(),
                rejectedReading.getVerifiedBy()
        );

        return MeterReadingMapper.toResponse(rejectedReading);
    }

    // ==============================================================
    // Private helper methods
    // ==============================================================

    /**
     * Current reading cannot be less than previous reading.
     */
    private void validateReadingValues(
            BigDecimal previousReading,
            BigDecimal currentReading
    ) {

        if (previousReading == null || currentReading == null) {

            throw new BadRequestException(
                    "Previous reading and current reading are required"
            );
        }

        if (currentReading.compareTo(previousReading) < 0) {

            throw new BadRequestException(
                    "Current reading cannot be less than previous reading"
            );
        }
    }

    /**
     * Calculates consumed units.
     */
    private BigDecimal calculateUnitsConsumed(
            BigDecimal previousReading,
            BigDecimal currentReading
    ) {

        return currentReading.subtract(previousReading);
    }

    /**
     * Allows admin operations only for ADMIN users.
     */
    private void requireAdminRole(String role) {

        if (!ADMIN_ROLE.equalsIgnoreCase(role)) {

            log.warn(
                    "Forbidden admin operation attempted by role: {}",
                    role
            );

            throw new ForbiddenOperationException(
                    "Only ADMIN users can perform this operation"
            );
        }
    }

    /**
     * Only submitted readings can be edited or deleted.
     */
    private void ensureEditable(MeterReading reading) {

        if (reading.getStatus() != MeterReadingStatus.SUBMITTED) {

            throw new ForbiddenOperationException(
                    "Meter reading with status "
                            + reading.getStatus()
                            + " cannot be modified"
            );
        }
    }

    /**
     * Finds a reading belonging to the authenticated consumer.
     */
    private MeterReading findOwnedReading(
            Long authUserId,
            Long readingId
    ) {

        return meterReadingRepository
                .findByIdAndAuthUserId(readingId, authUserId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "MeterReading",
                                "id",
                                readingId
                        )
                );
    }

    /**
     * Finds a reading by its database ID.
     */
    private MeterReading findReadingById(Long readingId) {

        return meterReadingRepository
                .findById(readingId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "MeterReading",
                                "id",
                                readingId
                        )
                );
    }
}