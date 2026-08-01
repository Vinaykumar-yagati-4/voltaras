package com.voltaras.meterreadingservice.service;

import com.voltaras.meterreadingservice.dto.request.RejectMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.UpdateMeterReadingRequest;
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
                .billingMonth(7)
                .billingYear(2026)
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .build();

        when(meterReadingRepository.existsByAuthUserIdAndMeterNumberAndBillingMonthAndBillingYear(
                eq(CONSUMER_ID), eq("MTR-001"), eq(7), eq(2026))).thenReturn(false);

        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> {
                    MeterReading reading = invocation.getArgument(0);
                    reading.setId(50L);
                    return reading;
                });

        MeterReadingResponse response = meterReadingService.submitReading(CONSUMER_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getAuthUserId()).isEqualTo(CONSUMER_ID);
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
                .billingMonth(7)
                .billingYear(2026)
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .build();

        when(meterReadingRepository.existsByAuthUserIdAndMeterNumberAndBillingMonthAndBillingYear(
                eq(CONSUMER_ID), eq("MTR-001"), eq(7), eq(2026))).thenReturn(true);

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
                .billingMonth(7)
                .billingYear(2026)
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

        MeterReading reading = buildSubmittedReading(50L);

        when(meterReadingRepository
                .findAllByAuthUserIdOrderByBillingYearDescBillingMonthDesc(CONSUMER_ID))
                .thenReturn(List.of(reading));

        List<MeterReadingResponse> response = meterReadingService.getMyReadings(CONSUMER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(50L);
        assertThat(response.getFirst().getAuthUserId()).isEqualTo(CONSUMER_ID);
    }

    @Test
    @DisplayName("Get my reading by id: another consumer's reading is not found")
    void getMyReadingById_foreignReading_throwsResourceNotFound() {

        when(meterReadingRepository.findByIdAndAuthUserId(50L, CONSUMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meterReadingService.getMyReadingById(CONSUMER_ID, 50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // ------------------------------------------------------------------
    // Update
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Update: submitted reading updated and units recalculated")
    void updateMyReading_submittedReading_success() {

        MeterReading reading = buildSubmittedReading(50L);

        when(meterReadingRepository.findByIdAndAuthUserId(50L, CONSUMER_ID))
                .thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMeterReadingRequest request = UpdateMeterReadingRequest.builder()
                .previousReading(new BigDecimal("1000.000"))
                .currentReading(new BigDecimal("1200.500"))
                .readingDate(LocalDate.of(2026, 8, 1))
                .build();

        MeterReadingResponse response =
                meterReadingService.updateMyReading(CONSUMER_ID, 50L, request);

        assertThat(response.getPreviousReading()).isEqualByComparingTo("1000.000");
        assertThat(response.getCurrentReading()).isEqualByComparingTo("1200.500");
        assertThat(response.getUnitsConsumed()).isEqualByComparingTo("200.500");
        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Update: verified reading cannot be edited")
    void updateMyReading_verifiedReading_throwsForbidden() {

        MeterReading verified = buildSubmittedReading(50L);
        verified.setStatus(MeterReadingStatus.VERIFIED);
        verified.setVerifiedBy(ADMIN_ID);
        verified.setVerifiedAt(java.time.LocalDateTime.now());

        when(meterReadingRepository.findByIdAndAuthUserId(50L, CONSUMER_ID))
                .thenReturn(Optional.of(verified));

        UpdateMeterReadingRequest request = UpdateMeterReadingRequest.builder()
                .previousReading(new BigDecimal("1000.000"))
                .currentReading(new BigDecimal("1200.500"))
                .readingDate(LocalDate.of(2026, 8, 1))
                .build();

        assertThatThrownBy(() -> meterReadingService.updateMyReading(CONSUMER_ID, 50L, request))
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

        MeterReading reading = buildSubmittedReading(50L);

        when(meterReadingRepository.findByIdAndAuthUserId(50L, CONSUMER_ID))
                .thenReturn(Optional.of(reading));

        meterReadingService.deleteMyReading(CONSUMER_ID, 50L);

        verify(meterReadingRepository).delete(reading);
    }

    @Test
    @DisplayName("Delete: rejected reading cannot be deleted")
    void deleteMyReading_rejectedReading_throwsForbidden() {

        MeterReading rejected = buildSubmittedReading(50L);
        rejected.setStatus(MeterReadingStatus.REJECTED);

        when(meterReadingRepository.findByIdAndAuthUserId(50L, CONSUMER_ID))
                .thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> meterReadingService.deleteMyReading(CONSUMER_ID, 50L))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(meterReadingRepository, never()).delete(any(MeterReading.class));
    }

    // ------------------------------------------------------------------
    // Admin operations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Admin verify: status VERIFIED with verifiedBy and verifiedAt stamped")
    void verifyReading_adminRole_success() {

        MeterReading reading = buildSubmittedReading(50L);

        when(meterReadingRepository.findById(50L)).thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeterReadingResponse response =
                meterReadingService.verifyReading(ADMIN_ID, "ADMIN", 50L);

        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.VERIFIED);
        assertThat(response.getVerifiedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin reject: status REJECTED with remarks, verifiedBy and verifiedAt")
    void rejectReading_adminRole_success() {

        MeterReading reading = buildSubmittedReading(50L);

        when(meterReadingRepository.findById(50L)).thenReturn(Optional.of(reading));
        when(meterReadingRepository.save(any(MeterReading.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RejectMeterReadingRequest request = RejectMeterReadingRequest.builder()
                .remarks("Meter reading exceeds expected consumption threshold")
                .build();

        MeterReadingResponse response =
                meterReadingService.rejectReading(ADMIN_ID, "ADMIN", 50L, request);

        assertThat(response.getStatus()).isEqualTo(MeterReadingStatus.REJECTED);
        assertThat(response.getRemarks()).isEqualTo(request.getRemarks());
        assertThat(response.getVerifiedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin verify: consumer role is rejected")
    void verifyReading_consumerRole_throwsForbidden() {

        assertThatThrownBy(() -> meterReadingService.verifyReading(CONSUMER_ID, "CONSUMER", 50L))
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

        MeterReading verified = buildSubmittedReading(50L);
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
    // Helpers
    // ------------------------------------------------------------------

    private MeterReading buildSubmittedReading(Long id) {

        return MeterReading.builder()
                .id(id)
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
