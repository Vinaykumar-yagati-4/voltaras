package com.voltaras.metermanagementservice.service;

import com.voltaras.metermanagementservice.dto.request.AssignMeterRequest;
import com.voltaras.metermanagementservice.dto.request.CreateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterStatusRequest;
import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.enums.MeterStatus;

import java.util.List;

/**
 * Business contract for meter management.
 *
 * <p>
 * Authenticated identity and role are always supplied by the caller
 * (read from the gateway-injected {@code X-User-Id} / {@code X-User-Role}
 * headers) — never parsed from the request body or URL.
 * </p>
 */
public interface MeterService {

    // ------------------------------------------------------------------
    // User APIs
    // ------------------------------------------------------------------

    /** Consumer: lists the meters assigned to the caller, newest first. */
    List<MeterSummaryResponse> getMyMeters(Long authUserId);

    /** Consumer: fetches one of the caller's own meters. */
    MeterResponse getMyMeterById(Long authUserId, Long meterId);

    // ------------------------------------------------------------------
    // Admin APIs
    // ------------------------------------------------------------------

    /** Admin: registers a new physical meter. */
    MeterResponse createMeter(Long adminUserId, String role, CreateMeterRequest request);

    /** Admin: lists all meters with optional filters. */
    List<MeterSummaryResponse> getAllMetersForAdmin(
            String role,
            MeterStatus status,
            Long authUserId,
            Long organizationId,
            String meterNumber
    );

    /** Admin: fetches one meter by ID. */
    MeterResponse getMeterByIdForAdmin(String role, Long meterId);

    /** Admin: updates meter details. */
    MeterResponse updateMeter(String role, Long meterId, UpdateMeterRequest request);

    /** Admin: assigns the meter to a consumer. */
    MeterResponse assignMeter(String role, Long meterId, AssignMeterRequest request);

    /** Admin: updates the meter status. */
    MeterResponse updateMeterStatus(String role, Long meterId, UpdateMeterStatusRequest request);

    /** Admin: soft-deletes the meter by setting its status to REMOVED. */
    void removeMeter(String role, Long meterId);
}
