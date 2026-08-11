package com.voltaras.metermanagementservice.service.impl;

import com.voltaras.metermanagementservice.dto.request.AssignMeterRequest;
import com.voltaras.metermanagementservice.dto.request.CreateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterStatusRequest;
import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.entity.Meter;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.exception.BadRequestException;
import com.voltaras.metermanagementservice.exception.DuplicateResourceException;
import com.voltaras.metermanagementservice.exception.ResourceNotFoundException;
import com.voltaras.metermanagementservice.mapper.MeterMapper;
import com.voltaras.metermanagementservice.repository.MeterRepository;
import com.voltaras.metermanagementservice.service.MeterService;
import com.voltaras.metermanagementservice.util.AdminRoleValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link MeterService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeterServiceImpl implements MeterService {

    private final MeterRepository meterRepository;

    // ------------------------------------------------------------------
    // User APIs
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<MeterSummaryResponse> getMyMeters(Long authUserId) {

        return meterRepository
                .findAllByAuthUserIdOrderByCreatedAtDesc(authUserId)
                .stream()
                .map(MeterMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeterResponse getMyMeterById(Long authUserId, Long meterId) {

        Meter meter = meterRepository
                .findByIdAndAuthUserId(meterId, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meter", "id", meterId));

        return MeterMapper.toResponse(meter);
    }

    // ------------------------------------------------------------------
    // Admin APIs
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public MeterResponse createMeter(Long adminUserId, String role, CreateMeterRequest request) {

        AdminRoleValidator.requireAdmin(role);

        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new DuplicateResourceException(
                    "Meter", "meterNumber", request.getMeterNumber());
        }

        Meter meter = MeterMapper.toEntity(request);

        meter.setStatus(request.getStatus() != null
                ? request.getStatus()
                : MeterStatus.ACTIVE);

        Meter saved = meterRepository.save(meter);

        log.info("Meter created: id={}, meterNumber={}, byAdmin={}",
                saved.getId(), saved.getMeterNumber(), adminUserId);

        return MeterMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterSummaryResponse> getAllMetersForAdmin(
            String role,
            MeterStatus status,
            Long authUserId,
            Long organizationId,
            String meterNumber) {

        AdminRoleValidator.requireAdmin(role);

        return meterRepository
                .findAllByFilters(status, authUserId, organizationId, meterNumber)
                .stream()
                .map(MeterMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeterResponse getMeterByIdForAdmin(String role, Long meterId) {

        AdminRoleValidator.requireAdmin(role);

        Meter meter = findMeterOrThrow(meterId);

        return MeterMapper.toResponse(meter);
    }

    @Override
    @Transactional
    public MeterResponse updateMeter(String role, Long meterId, UpdateMeterRequest request) {

        AdminRoleValidator.requireAdmin(role);

        Meter meter = findMeterOrThrow(meterId);

        MeterMapper.updateEntity(meter, request);

        Meter saved = meterRepository.save(meter);

        log.info("Meter {} updated", saved.getId());

        return MeterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MeterResponse assignMeter(String role, Long meterId, AssignMeterRequest request) {

        AdminRoleValidator.requireAdmin(role);

        Meter meter = findMeterOrThrow(meterId);

        if (meter.getStatus() == MeterStatus.REMOVED) {
            throw new BadRequestException(
                    "Cannot assign a REMOVED meter");
        }

        if (meter.getAuthUserId() != null
                && !meter.getAuthUserId().equals(request.getAuthUserId())) {
            throw new BadRequestException(
                    "Meter is already assigned to another user");
        }

        meter.setAuthUserId(request.getAuthUserId());

        // Only apply the organization link when provided, so an existing
        // organization is not silently cleared by a plain user assignment.
        if (request.getOrganizationId() != null) {
            meter.setOrganizationId(request.getOrganizationId());
        }

        Meter saved = meterRepository.save(meter);

        log.info("Meter {} assigned to user {}", saved.getId(), saved.getAuthUserId());

        return MeterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MeterResponse updateMeterStatus(
            String role, Long meterId, UpdateMeterStatusRequest request) {

        AdminRoleValidator.requireAdmin(role);

        Meter meter = findMeterOrThrow(meterId);

        meter.setStatus(request.getStatus());

        if (request.getRemarks() != null) {
            meter.setRemarks(request.getRemarks());
        }

        Meter saved = meterRepository.save(meter);

        log.info("Meter {} status changed to {}", saved.getId(), saved.getStatus());

        return MeterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeMeter(String role, Long meterId) {

        AdminRoleValidator.requireAdmin(role);

        Meter meter = findMeterOrThrow(meterId);

        // Soft delete: the row is kept, only the status changes to REMOVED.
        meter.setStatus(MeterStatus.REMOVED);

        meterRepository.save(meter);

        log.info("Meter {} soft-deleted (status REMOVED)", meterId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Meter findMeterOrThrow(Long meterId) {

        return meterRepository.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meter", "id", meterId));
    }
}
