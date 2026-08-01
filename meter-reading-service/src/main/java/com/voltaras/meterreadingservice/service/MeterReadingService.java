package com.voltaras.meterreadingservice.service;

import com.voltaras.meterreadingservice.dto.request.RejectMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.UpdateMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.response.MeterReadingResponse;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;

import java.util.List;

/**
 * Business contract for meter reading management.
 * <p>
 * Authenticated identity and role are always supplied by the caller
 * (read from the gateway-injected {@code X-User-Id} / {@code X-User-Role}
 * headers) — never parsed from the request body or URL.
 */
public interface MeterReadingService {

    /** Consumer: submits a new reading, starting in SUBMITTED status. */
    MeterReadingResponse submitReading(
            Long authUserId,
            SubmitMeterReadingRequest request
    );

    /** Consumer: lists the caller's own readings, newest billing period first. */
    List<MeterReadingResponse> getMyReadings(Long authUserId);

    /** Consumer: fetches one of the caller's own readings. */
    MeterReadingResponse getMyReadingById(Long authUserId, Long readingId);

    /** Consumer: updates an editable (SUBMITTED) reading owned by the caller. */
    MeterReadingResponse updateMyReading(
            Long authUserId,
            Long readingId,
            UpdateMeterReadingRequest request
    );

    /** Consumer: deletes an editable (SUBMITTED) reading owned by the caller. */
    void deleteMyReading(Long authUserId, Long readingId);

    /** Admin: lists all readings, optionally filtered by status. */
    List<MeterReadingResponse> getAllReadingsForAdmin(
            String role,
            MeterReadingStatus status
    );

    /** Admin: verifies a reading, stamping verifiedBy / verifiedAt. */
    MeterReadingResponse verifyReading(Long adminUserId, String role, Long readingId);

    /** Admin: rejects a reading with mandatory remarks, stamping verifiedBy / verifiedAt. */
    MeterReadingResponse rejectReading(
            Long adminUserId,
            String role,
            Long readingId,
            RejectMeterReadingRequest request
    );
}
