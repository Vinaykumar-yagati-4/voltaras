package com.voltaras.metermanagementservice.mapper;

import com.voltaras.metermanagementservice.dto.request.CreateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterRequest;
import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.entity.Meter;

/**
 * Converts between {@link Meter} entities and request/response DTOs.
 */
public final class MeterMapper {

    private MeterMapper() {
        // Prevent object creation for utility class
    }

    /**
     * Converts the create request into a new {@link Meter} entity.
     *
     * <p>
     * System-controlled fields such as status (defaults to ACTIVE),
     * authUserId and timestamps are assigned in the service layer.
     * </p>
     */
    public static Meter toEntity(CreateMeterRequest request) {

        return Meter.builder()
                .meterNumber(request.getMeterNumber())
                .meterType(request.getMeterType())
                .connectionType(request.getConnectionType())
                .phaseType(request.getPhaseType())
                .sanctionedLoadKw(request.getSanctionedLoadKw())
                .installationDate(request.getInstallationDate())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Applies only the non-null fields of the update request to the
     * existing entity. The meter number, ownership and status are
     * intentionally left untouched here.
     */
    public static void updateEntity(Meter meter, UpdateMeterRequest request) {

        if (request.getMeterType() != null) {
            meter.setMeterType(request.getMeterType());
        }

        if (request.getConnectionType() != null) {
            meter.setConnectionType(request.getConnectionType());
        }

        if (request.getPhaseType() != null) {
            meter.setPhaseType(request.getPhaseType());
        }

        if (request.getSanctionedLoadKw() != null) {
            meter.setSanctionedLoadKw(request.getSanctionedLoadKw());
        }

        if (request.getInstallationDate() != null) {
            meter.setInstallationDate(request.getInstallationDate());
        }

        if (request.getAddressLine() != null) {
            meter.setAddressLine(request.getAddressLine());
        }

        if (request.getCity() != null) {
            meter.setCity(request.getCity());
        }

        if (request.getState() != null) {
            meter.setState(request.getState());
        }

        if (request.getPincode() != null) {
            meter.setPincode(request.getPincode());
        }

        if (request.getRemarks() != null) {
            meter.setRemarks(request.getRemarks());
        }
    }

    /**
     * Converts an entity into the full API response DTO.
     */
    public static MeterResponse toResponse(Meter meter) {

        return MeterResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .authUserId(meter.getAuthUserId())
                .organizationId(meter.getOrganizationId())
                .meterType(meter.getMeterType())
                .connectionType(meter.getConnectionType())
                .phaseType(meter.getPhaseType())
                .status(meter.getStatus())
                .sanctionedLoadKw(meter.getSanctionedLoadKw())
                .installationDate(meter.getInstallationDate())
                .addressLine(meter.getAddressLine())
                .city(meter.getCity())
                .state(meter.getState())
                .pincode(meter.getPincode())
                .remarks(meter.getRemarks())
                .createdAt(meter.getCreatedAt())
                .updatedAt(meter.getUpdatedAt())
                .build();
    }

    /**
     * Converts an entity into the lightweight list response DTO.
     */
    public static MeterSummaryResponse toSummaryResponse(Meter meter) {

        return MeterSummaryResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .authUserId(meter.getAuthUserId())
                .organizationId(meter.getOrganizationId())
                .meterType(meter.getMeterType())
                .connectionType(meter.getConnectionType())
                .phaseType(meter.getPhaseType())
                .status(meter.getStatus())
                .sanctionedLoadKw(meter.getSanctionedLoadKw())
                .city(meter.getCity())
                .build();
    }
}
