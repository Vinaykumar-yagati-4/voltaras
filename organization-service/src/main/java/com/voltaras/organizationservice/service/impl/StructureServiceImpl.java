package com.voltaras.organizationservice.service.impl;

import com.voltaras.organizationservice.dto.request.CreateBlockRequest;
import com.voltaras.organizationservice.dto.request.CreateBuildingRequest;
import com.voltaras.organizationservice.dto.request.CreateFloorRequest;
import com.voltaras.organizationservice.dto.request.CreateUnitRequest;
import com.voltaras.organizationservice.dto.request.UpdateBlockRequest;
import com.voltaras.organizationservice.dto.request.UpdateBuildingRequest;
import com.voltaras.organizationservice.dto.request.UpdateFloorRequest;
import com.voltaras.organizationservice.dto.request.UpdateUnitRequest;
import com.voltaras.organizationservice.dto.request.UpdateUnitStatusRequest;
import com.voltaras.organizationservice.dto.response.BlockResponse;
import com.voltaras.organizationservice.dto.response.BuildingResponse;
import com.voltaras.organizationservice.dto.response.FloorResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.dto.response.UnitResponse;
import com.voltaras.organizationservice.entity.Block;
import com.voltaras.organizationservice.entity.Building;
import com.voltaras.organizationservice.entity.Floor;
import com.voltaras.organizationservice.entity.Unit;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.StructureStatus;
import com.voltaras.organizationservice.enums.UnitStatus;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.DuplicateResourceException;
import com.voltaras.organizationservice.exception.ResourceNotFoundException;
import com.voltaras.organizationservice.mapper.BlockMapper;
import com.voltaras.organizationservice.mapper.BuildingMapper;
import com.voltaras.organizationservice.mapper.FloorMapper;
import com.voltaras.organizationservice.mapper.UnitMapper;
import com.voltaras.organizationservice.repository.BlockRepository;
import com.voltaras.organizationservice.repository.BuildingRepository;
import com.voltaras.organizationservice.repository.FloorRepository;
import com.voltaras.organizationservice.repository.UnitRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.StructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Implements the physical structure hierarchy (buildings -> blocks ->
 * floors -> units). Every operation resolves the full parent chain back to
 * the organization and validates the caller's ACTIVE membership before any
 * write; reads additionally allow the system ADMIN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructureServiceImpl implements StructureService {

    private static final String DELETED_MESSAGE = "Resource deleted successfully";

    private final BuildingRepository buildingRepository;
    private final BlockRepository blockRepository;
    private final FloorRepository floorRepository;
    private final UnitRepository unitRepository;

    private final BuildingMapper buildingMapper;
    private final BlockMapper blockMapper;
    private final FloorMapper floorMapper;
    private final UnitMapper unitMapper;

    private final OrganizationAccessHelper accessHelper;

    // ==================================================================
    // Buildings
    // ==================================================================

    @Override
    @Transactional
    public BuildingResponse createBuilding(
            Long authUserId, Long organizationId, CreateBuildingRequest request) {

        requireStructureManager(organizationId, authUserId);

        String code = normalizeCode(request.getCode());

        if (buildingRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
            throw new DuplicateResourceException("Building", "code", code);
        }

        Building building = buildingMapper.toEntity(request);
        building.setCode(code);
        building.setOrganization(accessHelper.requireOrganization(organizationId));
        building.setStatus(StructureStatus.ACTIVE);

        Building saved = buildingRepository.save(building);

        log.info("Building created: buildingId={}, org={}, code={}, by={}",
                saved.getId(), organizationId, code, authUserId);

        return buildingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuildingResponse> getOrganizationBuildings(
            Long authUserId, String systemRole, Long organizationId) {

        requireReadAccess(organizationId, authUserId, systemRole);

        return buildingRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(buildingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingResponse getBuildingById(
            Long authUserId, String systemRole, Long buildingId) {

        Building building = requireBuilding(buildingId);

        requireReadAccess(
                building.getOrganization().getId(), authUserId, systemRole);

        return buildingMapper.toResponse(building);
    }

    @Override
    @Transactional
    public BuildingResponse updateBuilding(
            Long authUserId, Long buildingId, UpdateBuildingRequest request) {

        Building building = requireBuilding(buildingId);

        requireStructureManager(building.getOrganization().getId(), authUserId);

        buildingMapper.updateEntity(request, building);

        Building updated = buildingRepository.save(building);

        log.info("Building updated: buildingId={}, by={}", buildingId, authUserId);

        return buildingMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse deleteBuilding(Long authUserId, Long buildingId) {

        Building building = requireBuilding(buildingId);

        requireStructureManager(building.getOrganization().getId(), authUserId);

        if (blockRepository.existsByBuildingId(buildingId)) {
            throw new BadRequestException(
                    "Building cannot be deleted because it contains blocks");
        }

        buildingRepository.delete(building);

        log.info("Building deleted: buildingId={}, by={}", buildingId, authUserId);

        return MessageResponse.builder().message(DELETED_MESSAGE).build();
    }

    // ==================================================================
    // Blocks
    // ==================================================================

    @Override
    @Transactional
    public BlockResponse createBlock(
            Long authUserId, Long buildingId, CreateBlockRequest request) {

        Building building = requireBuilding(buildingId);

        requireStructureManager(building.getOrganization().getId(), authUserId);

        requireActiveParent(building.getStatus(), "Building");

        String code = normalizeCode(request.getCode());

        if (blockRepository.existsByBuildingIdAndCodeIgnoreCase(buildingId, code)) {
            throw new DuplicateResourceException("Block", "code", code);
        }

        Block block = blockMapper.toEntity(request);
        block.setCode(code);
        block.setBuilding(building);
        block.setStatus(StructureStatus.ACTIVE);

        Block saved = blockRepository.save(block);

        log.info("Block created: blockId={}, building={}, code={}, by={}",
                saved.getId(), buildingId, code, authUserId);

        return blockMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockResponse> getBuildingBlocks(
            Long authUserId, String systemRole, Long buildingId) {

        Building building = requireBuilding(buildingId);

        requireReadAccess(building.getOrganization().getId(), authUserId, systemRole);

        return blockRepository
                .findAllByBuildingIdOrderByCreatedAtDesc(buildingId)
                .stream()
                .map(blockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BlockResponse getBlockById(
            Long authUserId, String systemRole, Long blockId) {

        Block block = requireBlock(blockId);

        requireReadAccess(organizationIdOf(block), authUserId, systemRole);

        return blockMapper.toResponse(block);
    }

    @Override
    @Transactional
    public BlockResponse updateBlock(
            Long authUserId, Long blockId, UpdateBlockRequest request) {

        Block block = requireBlock(blockId);

        requireStructureManager(organizationIdOf(block), authUserId);

        blockMapper.updateEntity(request, block);

        Block updated = blockRepository.save(block);

        log.info("Block updated: blockId={}, by={}", blockId, authUserId);

        return blockMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse deleteBlock(Long authUserId, Long blockId) {

        Block block = requireBlock(blockId);

        requireStructureManager(organizationIdOf(block), authUserId);

        if (floorRepository.existsByBlockId(blockId)) {
            throw new BadRequestException(
                    "Block cannot be deleted because it contains floors");
        }

        blockRepository.delete(block);

        log.info("Block deleted: blockId={}, by={}", blockId, authUserId);

        return MessageResponse.builder().message(DELETED_MESSAGE).build();
    }

    // ==================================================================
    // Floors
    // ==================================================================

    @Override
    @Transactional
    public FloorResponse createFloor(
            Long authUserId, Long blockId, CreateFloorRequest request) {

        Block block = requireBlock(blockId);

        requireStructureManager(organizationIdOf(block), authUserId);

        requireActiveParent(block.getStatus(), "Block");

        if (floorRepository.existsByBlockIdAndFloorNumber(
                blockId, request.getFloorNumber())) {
            throw new DuplicateResourceException(
                    "Floor", "floorNumber", request.getFloorNumber());
        }

        Floor floor = floorMapper.toEntity(request);
        floor.setBlock(block);
        floor.setStatus(StructureStatus.ACTIVE);

        Floor saved = floorRepository.save(floor);

        log.info("Floor created: floorId={}, block={}, number={}, by={}",
                saved.getId(), blockId, request.getFloorNumber(), authUserId);

        return floorMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FloorResponse> getBlockFloors(
            Long authUserId, String systemRole, Long blockId) {

        Block block = requireBlock(blockId);

        requireReadAccess(organizationIdOf(block), authUserId, systemRole);

        return floorRepository
                .findAllByBlockIdOrderByFloorNumberAsc(blockId)
                .stream()
                .map(floorMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FloorResponse getFloorById(
            Long authUserId, String systemRole, Long floorId) {

        Floor floor = requireFloor(floorId);

        requireReadAccess(organizationIdOf(floor), authUserId, systemRole);

        return floorMapper.toResponse(floor);
    }

    @Override
    @Transactional
    public FloorResponse updateFloor(
            Long authUserId, Long floorId, UpdateFloorRequest request) {

        Floor floor = requireFloor(floorId);

        requireStructureManager(organizationIdOf(floor), authUserId);

        // floorNumber is immutable after creation and never applied by the mapper.
        floorMapper.updateEntity(request, floor);

        Floor updated = floorRepository.save(floor);

        log.info("Floor updated: floorId={}, by={}", floorId, authUserId);

        return floorMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse deleteFloor(Long authUserId, Long floorId) {

        Floor floor = requireFloor(floorId);

        requireStructureManager(organizationIdOf(floor), authUserId);

        if (unitRepository.existsByFloorId(floorId)) {
            throw new BadRequestException(
                    "Floor cannot be deleted because it contains units");
        }

        floorRepository.delete(floor);

        log.info("Floor deleted: floorId={}, by={}", floorId, authUserId);

        return MessageResponse.builder().message(DELETED_MESSAGE).build();
    }

    // ==================================================================
    // Units
    // ==================================================================

    @Override
    @Transactional
    public UnitResponse createUnit(
            Long authUserId, Long floorId, CreateUnitRequest request) {

        Floor floor = requireFloor(floorId);

        requireStructureManager(organizationIdOf(floor), authUserId);

        requireActiveParent(floor.getStatus(), "Floor");

        if (request.getCapacity() != null && request.getCapacity() < 0) {
            throw new BadRequestException("Capacity cannot be negative");
        }

        String unitNumber = normalizeCode(request.getUnitNumber());

        if (unitRepository.existsByFloorIdAndUnitNumberIgnoreCase(floorId, unitNumber)) {
            throw new DuplicateResourceException("Unit", "unitNumber", unitNumber);
        }

        Unit unit = unitMapper.toEntity(request);
        unit.setUnitNumber(unitNumber);
        unit.setFloor(floor);
        unit.setStatus(UnitStatus.AVAILABLE);

        Unit saved = unitRepository.save(unit);

        log.info("Unit created: unitId={}, floor={}, number={}, by={}",
                saved.getId(), floorId, unitNumber, authUserId);

        return unitMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> getFloorUnits(
            Long authUserId, String systemRole, Long floorId) {

        Floor floor = requireFloor(floorId);

        requireReadAccess(organizationIdOf(floor), authUserId, systemRole);

        return unitRepository
                .findAllByFloorIdOrderByUnitNumberAsc(floorId)
                .stream()
                .map(unitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnitById(
            Long authUserId, String systemRole, Long unitId) {

        Unit unit = requireUnit(unitId);

        requireReadAccess(organizationIdOf(unit), authUserId, systemRole);

        return unitMapper.toResponse(unit);
    }

    @Override
    @Transactional
    public UnitResponse updateUnit(
            Long authUserId, Long unitId, UpdateUnitRequest request) {

        Unit unit = requireUnit(unitId);

        requireStructureManager(organizationIdOf(unit), authUserId);

        unitMapper.updateEntity(request, unit);

        Unit updated = unitRepository.save(unit);

        log.info("Unit updated: unitId={}, by={}", unitId, authUserId);

        return unitMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public UnitResponse updateUnitStatus(
            Long authUserId, Long unitId, UpdateUnitStatusRequest request) {

        Unit unit = requireUnit(unitId);

        requireStructureManager(organizationIdOf(unit), authUserId);

        UnitStatus previousStatus = unit.getStatus();

        validateStatusTransition(previousStatus, request.getStatus());

        unit.setStatus(request.getStatus());

        Unit updated = unitRepository.save(unit);

        log.info("Unit status updated: unitId={}, {} -> {}, by={}",
                unitId, previousStatus, request.getStatus(), authUserId);

        return unitMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse deleteUnit(Long authUserId, Long unitId) {

        Unit unit = requireUnit(unitId);

        requireStructureManager(organizationIdOf(unit), authUserId);

        unitRepository.delete(unit);

        log.info("Unit deleted: unitId={}, by={}", unitId, authUserId);

        return MessageResponse.builder().message(DELETED_MESSAGE).build();
    }

    // ==================================================================
    // Private helpers
    // ==================================================================

    private Building requireBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Building", "id", buildingId));
    }

    private Block requireBlock(Long blockId) {
        return blockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block", "id", blockId));
    }

    private Floor requireFloor(Long floorId) {
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Floor", "id", floorId));
    }

    private Unit requireUnit(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unit", "id", unitId));
    }

    private Long organizationIdOf(Block block) {
        return block.getBuilding().getOrganization().getId();
    }

    private Long organizationIdOf(Floor floor) {
        return floor.getBlock().getBuilding().getOrganization().getId();
    }

    private Long organizationIdOf(Unit unit) {
        return unit.getFloor().getBlock().getBuilding().getOrganization().getId();
    }

    /**
     * Write access: ACTIVE membership with a management role, and the
     * organization must be ACTIVE.
     */
    private void requireStructureManager(Long organizationId, Long authUserId) {

        accessHelper.requireActiveOrganization(organizationId);

        accessHelper.requireOrganizationRole(
                organizationId, authUserId,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN,
                MembershipRole.MANAGER);
    }

    /**
     * Read access: any ACTIVE member, or the system ADMIN.
     */
    private void requireReadAccess(Long organizationId, Long authUserId, String systemRole) {

        if (accessHelper.isSystemAdmin(systemRole)) {
            return;
        }

        accessHelper.getActiveMembership(organizationId, authUserId);
    }

    /**
     * Children can only be created under an ACTIVE parent.
     */
    private void requireActiveParent(StructureStatus parentStatus, String resourceName) {

        if (parentStatus != StructureStatus.ACTIVE) {
            throw new BadRequestException(resourceName + " is not active");
        }
    }

    /**
     * Docs/10 §9.8 allowed transitions:
     * AVAILABLE ⇄ OCCUPIED, AVAILABLE ⇄ INACTIVE, AVAILABLE ⇄ MAINTENANCE,
     * OCCUPIED → MAINTENANCE. Any other transition is rejected.
     */
    private void validateStatusTransition(UnitStatus current, UnitStatus next) {

        if (current == next) {
            return;
        }

        boolean allowed = switch (current) {
            case AVAILABLE -> next == UnitStatus.OCCUPIED
                    || next == UnitStatus.INACTIVE
                    || next == UnitStatus.MAINTENANCE;
            case OCCUPIED -> next == UnitStatus.AVAILABLE
                    || next == UnitStatus.MAINTENANCE;
            case INACTIVE -> next == UnitStatus.AVAILABLE;
            case MAINTENANCE -> next == UnitStatus.AVAILABLE;
        };

        if (!allowed) {
            throw new BadRequestException(
                    "Invalid unit status transition: " + current + " -> " + next);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
