package com.voltaras.meterreadingservice.service;

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
import com.voltaras.meterreadingservice.repository.MeterReadingRepository;
import com.voltaras.meterreadingservice.service.impl.MeterReadingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MeterReadingServiceImpl} covering the
 * meter-reading business rules.
 */
@ExtendWith(MockitoExtension.class)
class MeterReadingServiceImplTest {

    private static final Long CONSUMER_ID = 100L;
    private static final Long ADMIN_ID = 1L;
    private static final Long READING_ID = 50L;

    @Mock
    private MeterReadingRepository meterReadingRepository;

    private MeterReadingServiceImpl meterReadingService;

    @BeforeEach
    void setUp() {
        meterReadingService = new MeterReadingServiceImpl(meterReadingRepository);
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Submit: success, status SUBMITTED and unitsConsumed calculated")
    void submitReading_success_calculatesUnitsAndSetsSubmittedStatus() {

        SubmitMeterReadingRequest request = SubmitMeterReadingRequest.builder()
                .meterNumber("MTR-001")
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .build();

        when(meterReadingRepository.existsByAuthUserIdAndMeterNumberAndReadingDate(
                eq(CONSUMER_ID), eq("MTR-001"), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(false);

        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> {
                    MeterReading reading = invocation.getArgument(0);
                    reading.setId(READING_ID);
                    return reading;
                });

        MeterReadingResponse response = meterReadingService.submitReading(CONSUMER_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(READING_ID);
        assertThat(response.getAuthUserId()).isEqualTo(CONSUMER_ID);
        assertThat(response.getBillingMonth()).isEqualTo(7);
        assertThat(response.getBillingYear()).isEqualTo(2026);
        assertThat(response.getUnitsConsumed()).isEqualByComparingTo("100.000");
        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.SUBMITTED);
        assertThat(response.getVerifiedBy()).isNull();
        assertThat(response.getVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("Submit: duplicate reading for same meter & billing period rejected")
    void submitReading_duplicateReading_throwsDuplicateResourceException() {

        SubmitMeterReadingRequest request = SubmitMeterReadingRequest.builder()
                .meterNumber("MTR-001")
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .build();

        when(meterReadingRepository.existsByAuthUserIdAndMeterNumberAndReadingDate(
                eq(CONSUMER_ID), eq("MTR-001"), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(true);

        assertThatThrownBy(() -> meterReadingService.submitReading(CONSUMER_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(meterReadingRepository, never()).save(any(MeterReading.class));
    }

    @Test
    @DisplayName("Submit: currentReading less than previousReading rejected")
    void submitReading_currentLessThanPrevious_throwsBadRequestException() {

        SubmitMeterReadingRequest request = SubmitMeterReadingRequest.builder()
                .meterNumber("MTR-001")
                .previousReading(new BigDecimal("1000.000"))
                .currentReading(new BigDecimal("900.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .build();

        assertThatThrownBy(() -> meterReadingService.submitReading(CONSUMER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current reading cannot be less than previous reading");

        verify(meterReadingRepository, never()).save(any(MeterReading.class));
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Get my readings: returns only the caller's readings")
    void getMyReadings_success_returnsOwnReadings() {

        MeterReading reading = buildSubmittedReading();

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(CONSUMER_ID))
                .thenReturn(List.of(reading));

        List<MeterReadingResponse> response = meterReadingService.getMyReadings(CONSUMER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(READING_ID);
        assertThat(response.getFirst().getAuthUserId()).isEqualTo(CONSUMER_ID);
    }

    @Test
    @DisplayName("Get my reading by id: another consumer's reading is not found")
    void getMyReadingById_foreignReading_throwsResourceNotFound() {

        when(meterReadingRepository.findByIdAndAuthUserId(READING_ID, CONSUMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meterReadingService.getMyReadingById(CONSUMER_ID, READING_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // ------------------------------------------------------------------
    // Update
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Update: submitted reading updated and units recalculated")
    void updateMyReading_submittedReading_success() {

        MeterReading reading = buildSubmittedReading();

        when(meterReadingRepository.findByIdAndAuthUserId(READING_ID, CONSUMER_ID))
                .thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMeterReadingRequest request = UpdateMeterReadingRequest.builder()
                .previousReading(new BigDecimal("1000.000"))
                .currentReading(new BigDecimal("1200.500"))
                .readingDate(LocalDate.of(2026, 8, 1))
                .build();

        MeterReadingResponse response =
                meterReadingService.updateMyReading(CONSUMER_ID, READING_ID, request);

        assertThat(response.getPreviousReading()).isEqualByComparingTo("1000.000");
        assertThat(response.getCurrentReading()).isEqualByComparingTo("1200.500");
        assertThat(response.getUnitsConsumed()).isEqualByComparingTo("200.500");
        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Update: verified reading cannot be edited")
    void updateMyReading_verifiedReading_throwsForbidden() {

        MeterReading verified = buildSubmittedReading();
        verified.setStatus(MeterReadingStatus.VERIFIED);
        verified.setVerifiedBy(ADMIN_ID);
        verified.setVerifiedAt(java.time.LocalDateTime.now());

        when(meterReadingRepository.findByIdAndAuthUserId(READING_ID, CONSUMER_ID))
                .thenReturn(Optional.of(verified));

        UpdateMeterReadingRequest request = UpdateMeterReadingRequest.builder()
                .previousReading(new BigDecimal("1000.000"))
                .currentReading(new BigDecimal("1200.500"))
                .readingDate(LocalDate.of(2026, 8, 1))
                .build();

        assertThatThrownBy(() -> meterReadingService.updateMyReading(CONSUMER_ID, READING_ID, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("cannot be modified");

        verify(meterReadingRepository, never()).save(any(MeterReading.class));
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Delete: submitted reading deleted")
    void deleteMyReading_submittedReading_success() {

        MeterReading reading = buildSubmittedReading();

        when(meterReadingRepository.findByIdAndAuthUserId(READING_ID, CONSUMER_ID))
                .thenReturn(Optional.of(reading));

        meterReadingService.deleteMyReading(CONSUMER_ID, READING_ID);

        verify(meterReadingRepository).delete(reading);
    }

    @Test
    @DisplayName("Delete: rejected reading cannot be deleted")
    void deleteMyReading_rejectedReading_throwsForbidden() {

        MeterReading rejected = buildSubmittedReading();
        rejected.setStatus(MeterReadingStatus.REJECTED);

        when(meterReadingRepository.findByIdAndAuthUserId(READING_ID, CONSUMER_ID))
                .thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> meterReadingService.deleteMyReading(CONSUMER_ID, READING_ID))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(meterReadingRepository, never()).delete(any(MeterReading.class));
    }

    // ------------------------------------------------------------------
    // Admin operations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Admin verify: status VERIFIED with verifiedBy and verifiedAt stamped")
    void verifyReading_adminRole_success() {

        MeterReading reading = buildSubmittedReading();

        when(meterReadingRepository.findById(READING_ID)).thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeterReadingResponse response =
                meterReadingService.verifyReading(ADMIN_ID, "ADMIN", READING_ID);

        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.VERIFIED);
        assertThat(response.getVerifiedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin reject: status REJECTED with remarks, verifiedBy and verifiedAt")
    void rejectReading_adminRole_success() {

        MeterReading reading = buildSubmittedReading();

        when(meterReadingRepository.findById(READING_ID)).thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RejectMeterReadingRequest request = RejectMeterReadingRequest.builder()
                .remarks("Meter reading exceeds expected consumption threshold")
                .build();

        MeterReadingResponse response =
                meterReadingService.rejectReading(ADMIN_ID, "ADMIN", READING_ID, request);

        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.REJECTED);
        assertThat(response.getRemarks()).isEqualTo(request.getRemarks());
        assertThat(response.getVerifiedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin verify: consumer role is rejected")
    void verifyReading_consumerRole_throwsForbidden() {

        assertThatThrownBy(() -> meterReadingService.verifyReading(CONSUMER_ID, "CONSUMER", READING_ID))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Only ADMIN");

        verify(meterReadingRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Admin verify: missing reading throws ResourceNotFound")
    void verifyReading_missingReading_throwsResourceNotFound() {

        when(meterReadingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meterReadingService.verifyReading(ADMIN_ID, "ADMIN", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Admin get all: filters by status when provided")
    void getAllReadingsForAdmin_statusFilter_returnsFilteredList() {

        MeterReading verified = buildSubmittedReading();
        verified.setStatus(MeterReadingStatus.VERIFIED);

        when(meterReadingRepository.findAllByStatusOrderByCreatedAtDesc(MeterReadingStatus.VERIFIED))
                .thenReturn(List.of(verified));

        List<MeterReadingResponse> response =
                meterReadingService.getAllReadingsForAdmin("ADMIN", MeterReadingStatus.VERIFIED);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getStatus()).isEqualTo(MeterReadingStatus.VERIFIED);
    }

    @Test
    @DisplayName("Admin get all: consumer role is rejected")
    void getAllReadingsForAdmin_consumerRole_throwsForbidden() {

        assertThatThrownBy(() ->
                meterReadingService.getAllReadingsForAdmin("CONSUMER", null))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Only ADMIN");
    }

    // ------------------------------------------------------------------
    // Daily usage tracking
    // ------------------------------------------------------------------

    /**
     * Fixed clock at 2026-08-14 so the daily usage assertions are
     * deterministic regardless of the real run date.
     */
    private static final java.time.Clock FIXED_CLOCK =
            java.time.Clock.fixed(
                    java.time.Instant.parse("2026-08-14T10:00:00Z"),
                    java.time.ZoneOffset.UTC
            );

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 14);

    @Test
    @DisplayName("Daily usage: today units, month units and 7-day series computed")
    void getDailyUsage_success_computesTodayMonthAndSeries() {

        meterReadingService.setClock(FIXED_CLOCK);

        List<MeterReading> readings = List.of(
                buildReading(1L, LocalDate.of(2026, 7, 15),
                        "1000.000", "1060.000", MeterReadingStatus.VERIFIED),
                buildReading(2L, LocalDate.of(2026, 8, 8),
                        "1060.000", "1069.000", MeterReadingStatus.VERIFIED),
                buildReading(3L, LocalDate.of(2026, 8, 9),
                        "1069.000", "1080.000", MeterReadingStatus.VERIFIED),
                buildReading(4L, LocalDate.of(2026, 8, 10),
                        "1080.000", "1088.000", MeterReadingStatus.VERIFIED),
                buildReading(5L, LocalDate.of(2026, 8, 11),
                        "1088.000", "1100.000", MeterReadingStatus.VERIFIED),
                buildReading(6L, LocalDate.of(2026, 8, 12),
                        "1100.000", "1110.000", MeterReadingStatus.VERIFIED),
                buildReading(7L, LocalDate.of(2026, 8, 13),
                        "1110.000", "1123.000", MeterReadingStatus.VERIFIED),
                buildReading(8L, LocalDate.of(2026, 8, 14),
                        "1123.000", "1134.000", MeterReadingStatus.VERIFIED)
        );

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(CONSUMER_ID))
                .thenReturn(readings);

        DailyUsageResponse response =
                meterReadingService.getDailyUsage(CONSUMER_ID);

        assertThat(response.isHasReadings()).isTrue();
        assertThat(response.isHasReadingToday()).isTrue();
        assertThat(response.getMeterNumber()).isEqualTo("MTR-001");
        assertThat(response.getUsageDate()).isEqualTo(TODAY);
        assertThat(response.getUnitsConsumedToday())
                .isEqualByComparingTo("11.00");
        assertThat(response.getPreviousReading())
                .isEqualByComparingTo("1123.000");
        assertThat(response.getLatestReading())
                .isEqualByComparingTo("1134.000");
        assertThat(response.getPreviousReadingAt())
                .isEqualTo(LocalDate.of(2026, 8, 13).atTime(10, 0));
        assertThat(response.getLatestReadingAt())
                .isEqualTo(TODAY.atTime(10, 0));
        assertThat(response.getMonthUnitsSoFar())
                .isEqualByComparingTo("74.00");
        assertThat(response.getEstimatedPerUnitCost())
                .isEqualByComparingTo("1.5000");
        assertThat(response.getEstimatedTodayCost())
                .isEqualByComparingTo("16.50");
        assertThat(response.getEstimatedMonthCost())
                .isEqualByComparingTo("221.55");

        assertThat(response.getDailyUsage()).hasSize(7);
        assertThat(response.getDailyUsage().getFirst().getDate())
                .isEqualTo(LocalDate.of(2026, 8, 8));
        assertThat(response.getDailyUsage().getFirst().getUnits())
                .isEqualByComparingTo("9.00");
        assertThat(response.getDailyUsage().getLast().getDate())
                .isEqualTo(TODAY);
        assertThat(response.getDailyUsage().getLast().getUnits())
                .isEqualByComparingTo("11.00");
    }

    @Test
    @DisplayName("Daily usage: no readings at all returns a clear empty state")
    void getDailyUsage_noReadings_returnsEmptyState() {

        meterReadingService.setClock(FIXED_CLOCK);

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(CONSUMER_ID))
                .thenReturn(List.of());

        DailyUsageResponse response =
                meterReadingService.getDailyUsage(CONSUMER_ID);

        assertThat(response.isHasReadings()).isFalse();
        assertThat(response.isHasReadingToday()).isFalse();
        assertThat(response.getUnitsConsumedToday())
                .isEqualByComparingTo("0.00");
        assertThat(response.getMonthUnitsSoFar())
                .isEqualByComparingTo("0.00");
        assertThat(response.getEstimatedMonthCost())
                .isEqualByComparingTo("0.00");
        assertThat(response.getDailyUsage()).isEmpty();
    }

    @Test
    @DisplayName("Daily usage: no reading today reports zero today units, month still computed")
    void getDailyUsage_noReadingToday_zeroTodayUnits() {

        meterReadingService.setClock(FIXED_CLOCK);

        List<MeterReading> readings = List.of(
                buildReading(1L, LocalDate.of(2026, 7, 15),
                        "1000.000", "1060.000", MeterReadingStatus.VERIFIED),
                buildReading(2L, LocalDate.of(2026, 8, 8),
                        "1060.000", "1069.000", MeterReadingStatus.VERIFIED),
                buildReading(3L, LocalDate.of(2026, 8, 13),
                        "1110.000", "1123.000", MeterReadingStatus.VERIFIED)
        );

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(CONSUMER_ID))
                .thenReturn(readings);

        DailyUsageResponse response =
                meterReadingService.getDailyUsage(CONSUMER_ID);

        assertThat(response.isHasReadings()).isTrue();
        assertThat(response.isHasReadingToday()).isFalse();
        assertThat(response.getUnitsConsumedToday())
                .isEqualByComparingTo("0.00");
        assertThat(response.getMonthUnitsSoFar())
                .isEqualByComparingTo("63.00");
        assertThat(response.getLatestReading())
                .isEqualByComparingTo("1123.000");
        // Missing days are reported as zero, not fabricated.
        assertThat(response.getDailyUsage().getLast().getUnits())
                .isEqualByComparingTo("0.00");
        assertThat(response.getDailyUsage().getLast().getReadingAt())
                .isNull();
    }

    @Test
    @DisplayName("Daily usage: rejected readings are excluded from the series")
    void getDailyUsage_rejectedReadingsExcluded() {

        meterReadingService.setClock(FIXED_CLOCK);

        List<MeterReading> readings = List.of(
                buildReading(1L, LocalDate.of(2026, 7, 15),
                        "1000.000", "1060.000", MeterReadingStatus.VERIFIED),
                buildReading(2L, LocalDate.of(2026, 8, 8),
                        "1060.000", "1069.000", MeterReadingStatus.VERIFIED),
                // Rejected reading on Aug 12 must be ignored entirely.
                buildReading(6L, LocalDate.of(2026, 8, 12),
                        "1100.000", "1500.000", MeterReadingStatus.REJECTED),
                buildReading(7L, LocalDate.of(2026, 8, 13),
                        "1110.000", "1123.000", MeterReadingStatus.VERIFIED),
                buildReading(8L, LocalDate.of(2026, 8, 14),
                        "1123.000", "1134.000", MeterReadingStatus.VERIFIED)
        );

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(CONSUMER_ID))
                .thenReturn(readings);

        DailyUsageResponse response =
                meterReadingService.getDailyUsage(CONSUMER_ID);

        // Aug 12 has no valid reading -> zero, and the next recorded value
        // is measured from the last valid reading (Aug 8), so Aug 13 carries
        // the Aug 9-13 gap (1123 - 1069 = 54).
        DailyUsageEntry aug12 = response.getDailyUsage().get(4);
        assertThat(aug12.getDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(aug12.getUnits()).isEqualByComparingTo("0.00");
        assertThat(aug12.getCurrentReading()).isNull();

        DailyUsageEntry aug13 = response.getDailyUsage().get(5);
        assertThat(aug13.getUnits()).isEqualByComparingTo("54.00");

        // Month total ignores the rejected reading.
        assertThat(response.getMonthUnitsSoFar())
                .isEqualByComparingTo("74.00");
    }

    @Test
    @DisplayName("Usage summary: window days clamped to 1..31")
    void getUsageSummary_clampsWindow() {

        meterReadingService.setClock(FIXED_CLOCK);

        List<MeterReading> readings = List.of(
                buildReading(1L, LocalDate.of(2026, 7, 15),
                        "1000.000", "1060.000", MeterReadingStatus.VERIFIED),
                buildReading(8L, LocalDate.of(2026, 8, 14),
                        "1123.000", "1134.000", MeterReadingStatus.VERIFIED)
        );

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByReadingDateDesc(CONSUMER_ID))
                .thenReturn(readings);

        DailyUsageResponse wide =
                meterReadingService.getUsageSummary(CONSUMER_ID, 99);
        assertThat(wide.getDailyUsage()).hasSize(31);

        DailyUsageResponse narrow =
                meterReadingService.getUsageSummary(CONSUMER_ID, 0);
        assertThat(narrow.getDailyUsage()).hasSize(1);
        assertThat(narrow.getDailyUsage().getFirst().getDate())
                .isEqualTo(TODAY);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MeterReading buildReading(
            Long id,
            LocalDate date,
            String previous,
            String current,
            MeterReadingStatus status
    ) {
        return MeterReading.builder()
                .id(id)
                .authUserId(CONSUMER_ID)
                .meterNumber("MTR-001")
                .billingMonth(date.getMonthValue())
                .billingYear(date.getYear())
                .previousReading(new BigDecimal(previous))
                .currentReading(new BigDecimal(current))
                .unitsConsumed(
                        new BigDecimal(current)
                                .subtract(new BigDecimal(previous))
                )
                .readingDate(date)
                .status(status)
                .createdAt(date.atTime(10, 0))
                .updatedAt(date.atTime(10, 0))
                .build();
    }

    private MeterReading buildSubmittedReading() {

        return MeterReading.builder()
                .id(READING_ID)
                .authUserId(CONSUMER_ID)
                .meterNumber("MTR-001")
                .billingMonth(7)
                .billingYear(2026)
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .unitsConsumed(new BigDecimal("100.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .status(MeterReadingStatus.SUBMITTED)
                .build();
    }
}