package com.voltaras.organizationservice.service;

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

import java.util.List;

/**
 * Business contract for the physical structure hierarchy:
 * buildings -> blocks -> floors -> units.
 * <p>
 * Authenticated identity is always supplied by the caller from the
 * gateway-injected {@code X-User-Id} header; systemRole is passed for read
 * endpoints where system ADMIN may also read.
 */
public interface StructureService {

    // ---------------- Buildings ----------------

    BuildingResponse createBuilding(
            Long authUserId, Long organizationId, CreateBuildingRequest request);

    List<BuildingResponse> getOrganizationBuildings(
            Long authUserId, String systemRole, Long organizationId);

    BuildingResponse getBuildingById(Long authUserId, String systemRole, Long buildingId);

    BuildingResponse updateBuilding(Long authUserId, Long buildingId, UpdateBuildingRequest request);

    MessageResponse deleteBuilding(Long authUserId, Long buildingId);

    // ---------------- Blocks ----------------

    BlockResponse createBlock(Long authUserId, Long buildingId, CreateBlockRequest request);

    List<BlockResponse> getBuildingBlocks(
            Long authUserId, String systemRole, Long buildingId);

    BlockResponse getBlockById(Long authUserId, String systemRole, Long blockId);

    BlockResponse updateBlock(Long authUserId, Long blockId, UpdateBlockRequest request);

    MessageResponse deleteBlock(Long authUserId, Long blockId);

    // ---------------- Floors ----------------

    FloorResponse createFloor(Long authUserId, Long blockId, CreateFloorRequest request);

    List<FloorResponse> getBlockFloors(Long authUserId, String systemRole, Long blockId);

    FloorResponse getFloorById(Long authUserId, String systemRole, Long floorId);

    FloorResponse updateFloor(Long authUserId, Long floorId, UpdateFloorRequest request);

    MessageResponse deleteFloor(Long authUserId, Long floorId);

    // ---------------- Units ----------------

    UnitResponse createUnit(Long authUserId, Long floorId, CreateUnitRequest request);

    List<UnitResponse> getFloorUnits(Long authUserId, String systemRole, Long floorId);

    UnitResponse getUnitById(Long authUserId, String systemRole, Long unitId);

    UnitResponse updateUnit(Long authUserId, Long unitId, UpdateUnitRequest request);

    UnitResponse updateUnitStatus(Long authUserId, Long unitId, UpdateUnitStatusRequest request);

    MessageResponse deleteUnit(Long authUserId, Long unitId);
}
