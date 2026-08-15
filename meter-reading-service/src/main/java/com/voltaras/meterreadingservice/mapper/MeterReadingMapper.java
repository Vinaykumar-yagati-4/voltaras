package com.voltaras.meterreadingservice.mapper;

import com.voltaras.meterreadingservice.dto.request.CreateAdminMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.UpdateMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.response.MeterReadingResponse;
import com.voltaras.meterreadingservice.entity.MeterReading;

public final class MeterReadingMapper {

    private MeterReadingMapper() {
        // Prevent object creation for utility class
    }

    /**
     * Converts the submit request into a new MeterReading entity.
     *
     * System-controlled fields such as authUserId, billingMonth,
     * billingYear, unitsConsumed and status are assigned in the
     * service layer.
     */
    public static MeterReading toEntity(
            SubmitMeterReadingRequest request
    ) {

        return MeterReading.builder()
                .meterNumber(request.getMeterNumber())
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Converts the admin-created reading request into a new MeterReading
     * entity. System-controlled fields such as authUserId, billingMonth,
     * billingYear, unitsConsumed and status are assigned in the service
     * layer.
     */
    public static MeterReading toEntity(
            CreateAdminMeterReadingRequest request
    ) {

        return MeterReading.builder()
                .meterNumber(request.getMeterNumber())
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Updates only consumer-editable fields.
     *
     * System-controlled fields such as authUserId, status,
     * verifiedBy and verifiedAt are not modified here.
     */
    public static void updateEntity(
            MeterReading reading,
            UpdateMeterReadingRequest request
    ) {

        reading.setPreviousReading(
                request.getPreviousReading()
        );

        reading.setCurrentReading(
                request.getCurrentReading()
        );

        reading.setReadingDate(
                request.getReadingDate()
        );

        reading.setRemarks(
                request.getRemarks()
        );
    }

    /**
     * Converts an entity into the API response DTO.
     */
    public static MeterReadingResponse toResponse(
            MeterReading reading
    ) {

        return MeterReadingResponse.builder()
                .id(reading.getId())
                .authUserId(reading.getAuthUserId())
                .meterNumber(reading.getMeterNumber())
                .billingMonth(reading.getBillingMonth())
                .billingYear(reading.getBillingYear())
                .previousReading(reading.getPreviousReading())
                .currentReading(reading.getCurrentReading())
                .unitsConsumed(reading.getUnitsConsumed())
                .readingDate(reading.getReadingDate())
                .status(reading.getStatus())
                .remarks(reading.getRemarks())
                .verifiedBy(reading.getVerifiedBy())
                .verifiedAt(reading.getVerifiedAt())
                .createdAt(reading.getCreatedAt())
                .updatedAt(reading.getUpdatedAt())
                .build();
    }
}