package com.voltaras.meterreadingservice.service.impl;

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
import com.voltaras.meterreadingservice.mapper.MeterReadingMapper;
import com.voltaras.meterreadingservice.repository.MeterReadingRepository;
import com.voltaras.meterreadingservice.service.MeterReadingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    private final MeterReadingRepository meterReadingRepository;

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