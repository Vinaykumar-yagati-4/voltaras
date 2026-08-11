package com.voltaras.metermanagementservice.service;

import com.voltaras.metermanagementservice.dto.request.AssignMeterRequest;
import com.voltaras.metermanagementservice.dto.request.CreateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterRequest;
import com.voltaras.metermanagementservice.dto.request.UpdateMeterStatusRequest;
import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.entity.Meter;
import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import com.voltaras.metermanagementservice.exception.AccessDeniedException;
import com.voltaras.metermanagementservice.exception.BadRequestException;
import com.voltaras.metermanagementservice.exception.DuplicateResourceException;
import com.voltaras.metermanagementservice.exception.ResourceNotFoundException;
import com.voltaras.metermanagementservice.repository.MeterRepository;
import com.voltaras.metermanagementservice.service.impl.MeterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MeterServiceImpl} covering the meter business rules.
 */
@ExtendWith(MockitoExtension.class)
class MeterServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long CONSUMER_ID = 100L;
    private static final Long METER_ID = 50L;

    @Mock
    private MeterRepository meterRepository;

    private MeterServiceImpl meterService;

    @BeforeEach
    void setUp() {
        meterService = new MeterServiceImpl(meterRepository);
    }

    // ------------------------------------------------------------------
    // User APIs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Get my meters: returns only the caller's meters as summaries")
    void getMyMeters_returnsOwnMeters() {

        when(meterRepository.findAllByAuthUserIdOrderByCreatedAtDesc(CONSUMER_ID))
                .thenReturn(List.of(buildMeter(MeterStatus.ACTIVE, CONSUMER_ID, null)));

        List<MeterSummaryResponse> response = meterService.getMyMeters(CONSUMER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo(METER_ID);
        assertThat(response.getFirst().getMeterNumber()).isEqualTo("MTR-001");
        assertThat(response.getFirst().getAuthUserId()).isEqualTo(CONSUMER_ID);
    }

    @Test
    @DisplayName("Get my meter by id: another consumer's meter is not found")
    void getMyMeterById_foreignMeter_throwsResourceNotFound() {

        when(meterRepository.findByIdAndAuthUserId(METER_ID, CONSUMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meterService.getMyMeterById(CONSUMER_ID, METER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Get my meter by id: owned meter returned with full details")
    void getMyMeterById_ownedMeter_success() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, CONSUMER_ID, null);

        when(meterRepository.findByIdAndAuthUserId(METER_ID, CONSUMER_ID))
                .thenReturn(Optional.of(meter));

        MeterResponse response = meterService.getMyMeterById(CONSUMER_ID, METER_ID);

        assertThat(response.getId()).isEqualTo(METER_ID);
        assertThat(response.getAuthUserId()).isEqualTo(CONSUMER_ID);
        assertThat(response.getMeterType()).isEqualTo(MeterType.SMART);
    }

    // ------------------------------------------------------------------
    // Admin APIs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create: success with ACTIVE default status")
    void createMeter_success_defaultsToActive() {

        CreateMeterRequest request = buildCreateRequest(null);

        when(meterRepository.existsByMeterNumber("MTR-001")).thenReturn(false);
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> {
                    Meter meter = invocation.getArgument(0);
                    meter.setId(METER_ID);
                    return meter;
                });

        MeterResponse response = meterService.createMeter(ADMIN_ID, "ADMIN", request);

        assertThat(response.getId()).isEqualTo(METER_ID);
        assertThat(response.getMeterNumber()).isEqualTo("MTR-001");
        assertThat(response.getStatus()).isEqualTo(MeterStatus.ACTIVE);
    }

    @Test
    @DisplayName("Create: explicit status is honored")
    void createMeter_withExplicitStatus() {

        CreateMeterRequest request = buildCreateRequest(MeterStatus.INACTIVE);

        when(meterRepository.existsByMeterNumber("MTR-001")).thenReturn(false);
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeterResponse response = meterService.createMeter(ADMIN_ID, "ADMIN", request);

        assertThat(response.getStatus()).isEqualTo(MeterStatus.INACTIVE);
    }

    @Test
    @DisplayName("Create: duplicate meter number throws DuplicateResourceException")
    void createMeter_duplicateMeterNumber_throwsDuplicate() {

        CreateMeterRequest request = buildCreateRequest(null);

        when(meterRepository.existsByMeterNumber("MTR-001")).thenReturn(true);

        assertThatThrownBy(() -> meterService.createMeter(ADMIN_ID, "ADMIN", request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(meterRepository, never()).save(any(Meter.class));
    }

    @Test
    @DisplayName("Create: consumer role is rejected")
    void createMeter_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() ->
                meterService.createMeter(CONSUMER_ID, "CONSUMER", buildCreateRequest(null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only ADMIN");
    }

    @Test
    @DisplayName("Admin get all: filters are forwarded to the repository")
    void getAllMetersForAdmin_forwardsFilters() {

        when(meterRepository.findAllByFilters(
                eq(MeterStatus.ACTIVE), eq(CONSUMER_ID), eq(7L), eq("MTR-001")))
                .thenReturn(List.of(buildMeter(MeterStatus.ACTIVE, CONSUMER_ID, 7L)));

        List<MeterSummaryResponse> response = meterService.getAllMetersForAdmin(
                "ADMIN", MeterStatus.ACTIVE, CONSUMER_ID, 7L, "MTR-001");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getOrganizationId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Admin get all: consumer role is rejected")
    void getAllMetersForAdmin_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() ->
                meterService.getAllMetersForAdmin("CONSUMER", null, null, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only ADMIN");
    }

    @Test
    @DisplayName("Admin get by id: missing meter throws ResourceNotFound")
    void getMeterByIdForAdmin_missingMeter_throwsResourceNotFound() {

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meterService.getMeterByIdForAdmin("ADMIN", METER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Update: applies provided fields only")
    void updateMeter_appliesProvidedFields() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, CONSUMER_ID, null);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMeterRequest request = UpdateMeterRequest.builder()
                .city("Chennai")
                .sanctionedLoadKw(new BigDecimal("10.000"))
                .build();

        MeterResponse response = meterService.updateMeter("ADMIN", METER_ID, request);

        assertThat(response.getCity()).isEqualTo("Chennai");
        assertThat(response.getSanctionedLoadKw()).isEqualByComparingTo("10.000");
        // Untouched fields stay as they were
        assertThat(response.getMeterNumber()).isEqualTo("MTR-001");
        assertThat(response.getMeterType()).isEqualTo(MeterType.SMART);
    }

    @Test
    @DisplayName("Update: missing meter throws ResourceNotFound")
    void updateMeter_missingMeter_throwsResourceNotFound() {

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                meterService.updateMeter("ADMIN", METER_ID, new UpdateMeterRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Assign: success sets authUserId and organizationId")
    void assignMeter_success() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, null, null);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssignMeterRequest request = AssignMeterRequest.builder()
                .authUserId(CONSUMER_ID)
                .organizationId(7L)
                .build();

        MeterResponse response = meterService.assignMeter("ADMIN", METER_ID, request);

        assertThat(response.getAuthUserId()).isEqualTo(CONSUMER_ID);
        assertThat(response.getOrganizationId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Assign: omitted organizationId keeps the existing organization link")
    void assignMeter_keepsExistingOrganizationWhenNotProvided() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, null, 7L);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssignMeterRequest request = AssignMeterRequest.builder()
                .authUserId(CONSUMER_ID)
                .build();

        MeterResponse response = meterService.assignMeter("ADMIN", METER_ID, request);

        assertThat(response.getAuthUserId()).isEqualTo(CONSUMER_ID);
        assertThat(response.getOrganizationId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Assign: already assigned to another user is rejected")
    void assignMeter_alreadyAssignedToOther_throwsBadRequest() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, 200L, null);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));

        AssignMeterRequest request = AssignMeterRequest.builder()
                .authUserId(CONSUMER_ID)
                .build();

        assertThatThrownBy(() -> meterService.assignMeter("ADMIN", METER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already assigned");

        verify(meterRepository, never()).save(any(Meter.class));
    }

    @Test
    @DisplayName("Assign: removed meter cannot be assigned")
    void assignMeter_removedMeter_throwsBadRequest() {

        Meter meter = buildMeter(MeterStatus.REMOVED, null, null);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));

        AssignMeterRequest request = AssignMeterRequest.builder()
                .authUserId(CONSUMER_ID)
                .build();

        assertThatThrownBy(() -> meterService.assignMeter("ADMIN", METER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("REMOVED");
    }

    @Test
    @DisplayName("Update status: status and remarks applied")
    void updateMeterStatus_success() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, CONSUMER_ID, null);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMeterStatusRequest request = UpdateMeterStatusRequest.builder()
                .status(MeterStatus.FAULTY)
                .remarks("Display not working")
                .build();

        MeterResponse response = meterService.updateMeterStatus("ADMIN", METER_ID, request);

        assertThat(response.getStatus()).isEqualTo(MeterStatus.FAULTY);
        assertThat(response.getRemarks()).isEqualTo("Display not working");
    }

    @Test
    @DisplayName("Remove: soft delete sets status to REMOVED")
    void removeMeter_setsStatusRemoved() {

        Meter meter = buildMeter(MeterStatus.ACTIVE, CONSUMER_ID, null);

        when(meterRepository.findById(METER_ID)).thenReturn(Optional.of(meter));
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        meterService.removeMeter("ADMIN", METER_ID);

        assertThat(meter.getStatus()).isEqualTo(MeterStatus.REMOVED);
        verify(meterRepository).save(meter);
    }

    @Test
    @DisplayName("Remove: consumer role is rejected")
    void removeMeter_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() -> meterService.removeMeter("CONSUMER", METER_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(meterRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("ROLE_ADMIN spelling is accepted")
    void adminOperations_acceptRoleAdminSpelling() {

        when(meterRepository.findById(METER_ID))
                .thenReturn(Optional.of(buildMeter(MeterStatus.ACTIVE, null, null)));
        when(meterRepository.save(any(Meter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeterResponse response =
                meterService.updateMeterStatus("ROLE_ADMIN", METER_ID,
                        UpdateMeterStatusRequest.builder()
                                .status(MeterStatus.INACTIVE)
                                .build());

        assertThat(response.getStatus()).isEqualTo(MeterStatus.INACTIVE);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private CreateMeterRequest buildCreateRequest(MeterStatus status) {

        return CreateMeterRequest.builder()
                .meterNumber("MTR-001")
                .meterType(MeterType.SMART)
                .connectionType(ConnectionType.RESIDENTIAL)
                .phaseType(PhaseType.SINGLE_PHASE)
                .status(status)
                .sanctionedLoadKw(new BigDecimal("5.000"))
                .city("Bengaluru")
                .pincode("560001")
                .build();
    }

    private Meter buildMeter(MeterStatus status, Long authUserId, Long organizationId) {

        return Meter.builder()
                .id(METER_ID)
                .meterNumber("MTR-001")
                .authUserId(authUserId)
                .organizationId(organizationId)
                .meterType(MeterType.SMART)
                .connectionType(ConnectionType.RESIDENTIAL)
                .phaseType(PhaseType.SINGLE_PHASE)
                .status(status)
                .sanctionedLoadKw(new BigDecimal("5.000"))
                .city("Bengaluru")
                .pincode("560001")
                .build();
    }
}
