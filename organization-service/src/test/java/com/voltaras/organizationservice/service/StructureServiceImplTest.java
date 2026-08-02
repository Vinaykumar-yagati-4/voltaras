package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.CreateBlockRequest;
import com.voltaras.organizationservice.dto.request.CreateBuildingRequest;
import com.voltaras.organizationservice.dto.request.CreateFloorRequest;
import com.voltaras.organizationservice.dto.request.CreateUnitRequest;
import com.voltaras.organizationservice.dto.request.UpdateUnitStatusRequest;
import com.voltaras.organizationservice.dto.response.BuildingResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.dto.response.UnitResponse;
import com.voltaras.organizationservice.entity.Block;
import com.voltaras.organizationservice.entity.Building;
import com.voltaras.organizationservice.entity.Floor;
import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.entity.Unit;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import com.voltaras.organizationservice.enums.StructureStatus;
import com.voltaras.organizationservice.enums.UnitStatus;
import com.voltaras.organizationservice.enums.UnitType;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.DuplicateResourceException;
import com.voltaras.organizationservice.exception.ForbiddenOperationException;
import com.voltaras.organizationservice.mapper.BlockMapper;
import com.voltaras.organizationservice.mapper.BuildingMapper;
import com.voltaras.organizationservice.mapper.FloorMapper;
import com.voltaras.organizationservice.mapper.UnitMapper;
import com.voltaras.organizationservice.repository.BlockRepository;
import com.voltaras.organizationservice.repository.BuildingRepository;
import com.voltaras.organizationservice.repository.FloorRepository;
import com.voltaras.organizationservice.repository.UnitRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.impl.StructureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StructureServiceImpl} covering the building ->
 * block -> floor -> unit hierarchy rules.
 */
@ExtendWith(MockitoExtension.class)
class StructureServiceImplTest {

    private static final Long MANAGER_ID = 100L;
    private static final Long ORG_ID = 1L;
    private static final Long BUILDING_ID = 10L;
    private static final Long BLOCK_ID = 20L;
    private static final Long FLOOR_ID = 30L;
    private static final Long UNIT_ID = 40L;

    @Mock private BuildingRepository buildingRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private BuildingMapper buildingMapper;
    @Mock private BlockMapper blockMapper;
    @Mock private FloorMapper floorMapper;
    @Mock private UnitMapper unitMapper;
    @Mock private OrganizationAccessHelper accessHelper;

    private StructureServiceImpl structureService;

    @BeforeEach
    void setUp() {
        structureService = new StructureServiceImpl(
                buildingRepository, blockRepository, floorRepository, unitRepository,
                buildingMapper, blockMapper, floorMapper, unitMapper, accessHelper);
    }

    // ------------------------------------------------------------------
    // Buildings
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create building: code normalized and saved")
    void createBuilding_success() {

        CreateBuildingRequest request = CreateBuildingRequest.builder()
                .name("Main Building")
                .code("main")
                .build();

        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(buildOrganization());
        when(buildingRepository.existsByOrganizationIdAndCodeIgnoreCase(ORG_ID, "MAIN"))
                .thenReturn(false);
        when(buildingMapper.toEntity(request)).thenReturn(Building.builder().build());
        when(buildingRepository.save(any(Building.class))).thenAnswer(invocation -> {
            Building building = invocation.getArgument(0);
            building.setId(BUILDING_ID);
            return building;
        });
        when(buildingMapper.toResponse(any())).thenReturn(BuildingResponse.builder()
                .id(BUILDING_ID).code("MAIN").build());

        BuildingResponse response =
                structureService.createBuilding(MANAGER_ID, ORG_ID, request);

        assertThat(response.getCode()).isEqualTo("MAIN");

        verify(buildingRepository).save(argThat(b ->
                b.getCode().equals("MAIN")
                        && b.getStatus() == StructureStatus.ACTIVE));
    }

    @Test
    @DisplayName("Create building: duplicate code rejected")
    void createBuilding_duplicateCode_throwsDuplicate() {

        CreateBuildingRequest request = CreateBuildingRequest.builder()
                .name("Main Building")
                .code("MAIN")
                .build();

        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(buildOrganization());
        when(buildingRepository.existsByOrganizationIdAndCodeIgnoreCase(ORG_ID, "MAIN"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                structureService.createBuilding(MANAGER_ID, ORG_ID, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(buildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create building: inactive organization rejected")
    void createBuilding_inactiveOrg_throwsBadRequest() {

        CreateBuildingRequest request = CreateBuildingRequest.builder()
                .name("Main Building")
                .code("MAIN")
                .build();

        doThrow(new BadRequestException("Organization is not active"))
                .when(accessHelper).requireActiveOrganization(ORG_ID);

        assertThatThrownBy(() ->
                structureService.createBuilding(MANAGER_ID, ORG_ID, request))
                .isInstanceOf(BadRequestException.class);

        verify(buildingRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create block: code unique within building")
    void createBlock_success() {

        CreateBlockRequest request = CreateBlockRequest.builder()
                .name("Block A")
                .code("a")
                .build();

        Building building = buildBuilding();

        when(buildingRepository.findById(BUILDING_ID)).thenReturn(Optional.of(building));
        when(blockRepository.existsByBuildingIdAndCodeIgnoreCase(BUILDING_ID, "A"))
                .thenReturn(false);
        when(blockMapper.toEntity(request)).thenReturn(Block.builder().build());
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> {
            Block block = invocation.getArgument(0);
            block.setId(BLOCK_ID);
            return block;
        });

        structureService.createBlock(MANAGER_ID, BUILDING_ID, request);

        verify(blockRepository).save(argThat(b ->
                b.getCode().equals("A") && b.getBuilding().getId().equals(BUILDING_ID)));
    }

    @Test
    @DisplayName("Create block: parent building not ACTIVE rejected")
    void createBlock_inactiveBuilding_throwsBadRequest() {

        CreateBlockRequest request = CreateBlockRequest.builder()
                .name("Block A")
                .code("A")
                .build();

        Building building = buildBuilding();
        building.setStatus(StructureStatus.MAINTENANCE);

        when(buildingRepository.findById(BUILDING_ID)).thenReturn(Optional.of(building));

        assertThatThrownBy(() ->
                structureService.createBlock(MANAGER_ID, BUILDING_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");

        verify(blockRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Floors
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create floor: duplicate floor number rejected")
    void createFloor_duplicateNumber_throwsDuplicate() {

        CreateFloorRequest request = CreateFloorRequest.builder()
                .floorNumber(1)
                .build();

        Block block = buildBlock();

        when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(block));
        when(floorRepository.existsByBlockIdAndFloorNumber(BLOCK_ID, 1)).thenReturn(true);

        assertThatThrownBy(() ->
                structureService.createFloor(MANAGER_ID, BLOCK_ID, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(floorRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Units
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create unit: negative capacity rejected")
    void createUnit_negativeCapacity_throwsBadRequest() {

        CreateUnitRequest request = CreateUnitRequest.builder()
                .unitNumber("101")
                .unitType(UnitType.ROOM)
                .capacity(-1)
                .build();

        Floor floor = buildFloor();

        when(floorRepository.findById(FLOOR_ID)).thenReturn(Optional.of(floor));

        assertThatThrownBy(() ->
                structureService.createUnit(MANAGER_ID, FLOOR_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negative");

        verify(unitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create unit: duplicate unit number rejected")
    void createUnit_duplicateNumber_throwsDuplicate() {

        CreateUnitRequest request = CreateUnitRequest.builder()
                .unitNumber("101")
                .unitType(UnitType.ROOM)
                .capacity(2)
                .build();

        Floor floor = buildFloor();

        when(floorRepository.findById(FLOOR_ID)).thenReturn(Optional.of(floor));
        when(unitRepository.existsByFloorIdAndUnitNumberIgnoreCase(FLOOR_ID, "101"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                structureService.createUnit(MANAGER_ID, FLOOR_ID, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(unitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update unit status: invalid transition rejected")
    void updateUnitStatus_invalidTransition_throwsBadRequest() {

        Unit unit = buildUnit();
        unit.setStatus(UnitStatus.OCCUPIED);

        when(unitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> structureService.updateUnitStatus(
                MANAGER_ID, UNIT_ID,
                UpdateUnitStatusRequest.builder().status(UnitStatus.INACTIVE).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid unit status transition");

        verify(unitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update unit status: AVAILABLE -> OCCUPIED allowed")
    void updateUnitStatus_validTransition_success() {

        Unit unit = buildUnit();
        unit.setStatus(UnitStatus.AVAILABLE);

        when(unitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(unitRepository.save(unit)).thenReturn(unit);
        when(unitMapper.toResponse(unit)).thenReturn(UnitResponse.builder()
                .id(UNIT_ID).status(UnitStatus.OCCUPIED).build());

        UnitResponse response = structureService.updateUnitStatus(
                MANAGER_ID, UNIT_ID,
                UpdateUnitStatusRequest.builder().status(UnitStatus.OCCUPIED).build());

        assertThat(response.getStatus()).isEqualTo(UnitStatus.OCCUPIED);
    }

    // ------------------------------------------------------------------
    // Deletes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Delete building: blocked when blocks exist")
    void deleteBuilding_withBlocks_throwsBadRequest() {

        when(buildingRepository.findById(BUILDING_ID))
                .thenReturn(Optional.of(buildBuilding()));
        when(blockRepository.existsByBuildingId(BUILDING_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                structureService.deleteBuilding(MANAGER_ID, BUILDING_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("contains blocks");

        verify(buildingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete floor: blocked when units exist")
    void deleteFloor_withUnits_throwsBadRequest() {

        when(floorRepository.findById(FLOOR_ID)).thenReturn(Optional.of(buildFloor()));
        when(unitRepository.existsByFloorId(FLOOR_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                structureService.deleteFloor(MANAGER_ID, FLOOR_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("contains units");
    }

    @Test
    @DisplayName("Delete unit: returns confirmation message")
    void deleteUnit_success_returnsMessage() {

        when(unitRepository.findById(UNIT_ID)).thenReturn(Optional.of(buildUnit()));

        MessageResponse response = structureService.deleteUnit(MANAGER_ID, UNIT_ID);

        assertThat(response.getMessage()).isEqualTo("Resource deleted successfully");
        verify(unitRepository).delete(any(Unit.class));
    }

    // ------------------------------------------------------------------
    // Read authorization
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Get building by id: non-member is forbidden")
    void getBuildingById_nonMember_throwsForbidden() {

        when(buildingRepository.findById(BUILDING_ID))
                .thenReturn(Optional.of(buildBuilding()));
        when(accessHelper.isSystemAdmin("CONSUMER")).thenReturn(false);
        when(accessHelper.getActiveMembership(ORG_ID, MANAGER_ID))
                .thenThrow(new ForbiddenOperationException(
                        "You are not an active member of this organization"));

        assertThatThrownBy(() ->
                structureService.getBuildingById(MANAGER_ID, "CONSUMER", BUILDING_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Organization buildOrganization() {
        return Organization.builder()
                .id(ORG_ID)
                .name("Sunrise Hostel")
                .organizationCode("SUNRISE-HST")
                .organizationType(OrganizationType.HOSTEL)
                .status(OrganizationStatus.ACTIVE)
                .build();
    }

    private Building buildBuilding() {
        return Building.builder()
                .id(BUILDING_ID)
                .organization(buildOrganization())
                .name("Main Building")
                .code("MAIN")
                .status(StructureStatus.ACTIVE)
                .build();
    }

    private Block buildBlock() {
        return Block.builder()
                .id(BLOCK_ID)
                .building(buildBuilding())
                .name("Block A")
                .code("A")
                .status(StructureStatus.ACTIVE)
                .build();
    }

    private Floor buildFloor() {
        return Floor.builder()
                .id(FLOOR_ID)
                .block(buildBlock())
                .floorNumber(1)
                .status(StructureStatus.ACTIVE)
                .build();
    }

    private Unit buildUnit() {
        return Unit.builder()
                .id(UNIT_ID)
                .floor(buildFloor())
                .unitNumber("101")
                .unitType(UnitType.ROOM)
                .capacity(2)
                .status(UnitStatus.AVAILABLE)
                .build();
    }
}
