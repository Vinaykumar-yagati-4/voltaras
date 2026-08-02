package com.voltaras.organizationservice.controller;

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
import com.voltaras.organizationservice.service.StructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StructureController {

    private final StructureService structureService;

    // ==================================================================
    // Buildings
    // ==================================================================

    @PostMapping("/organizations/{organizationId}/buildings")
    public ResponseEntity<BuildingResponse> createBuilding(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateBuildingRequest request) {

        BuildingResponse response =
                structureService.createBuilding(authUserId, organizationId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/organizations/{organizationId}/buildings")
    public ResponseEntity<List<BuildingResponse>> getOrganizationBuildings(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId) {

        List<BuildingResponse> response =
                structureService.getOrganizationBuildings(
                        authUserId, systemRole, organizationId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/buildings/{buildingId}")
    public ResponseEntity<BuildingResponse> getBuildingById(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long buildingId) {

        BuildingResponse response =
                structureService.getBuildingById(authUserId, systemRole, buildingId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/buildings/{buildingId}")
    public ResponseEntity<BuildingResponse> updateBuilding(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long buildingId,
            @Valid @RequestBody UpdateBuildingRequest request) {

        BuildingResponse response =
                structureService.updateBuilding(authUserId, buildingId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/buildings/{buildingId}")
    public ResponseEntity<MessageResponse> deleteBuilding(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long buildingId) {

        MessageResponse response =
                structureService.deleteBuilding(authUserId, buildingId);

        return ResponseEntity.ok(response);
    }

    // ==================================================================
    // Blocks
    // ==================================================================

    @PostMapping("/buildings/{buildingId}/blocks")
    public ResponseEntity<BlockResponse> createBlock(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long buildingId,
            @Valid @RequestBody CreateBlockRequest request) {

        BlockResponse response =
                structureService.createBlock(authUserId, buildingId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/buildings/{buildingId}/blocks")
    public ResponseEntity<List<BlockResponse>> getBuildingBlocks(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long buildingId) {

        List<BlockResponse> response =
                structureService.getBuildingBlocks(authUserId, systemRole, buildingId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/blocks/{blockId}")
    public ResponseEntity<BlockResponse> getBlockById(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long blockId) {

        BlockResponse response =
                structureService.getBlockById(authUserId, systemRole, blockId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/blocks/{blockId}")
    public ResponseEntity<BlockResponse> updateBlock(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long blockId,
            @Valid @RequestBody UpdateBlockRequest request) {

        BlockResponse response =
                structureService.updateBlock(authUserId, blockId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/blocks/{blockId}")
    public ResponseEntity<MessageResponse> deleteBlock(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long blockId) {

        MessageResponse response =
                structureService.deleteBlock(authUserId, blockId);

        return ResponseEntity.ok(response);
    }

    // ==================================================================
    // Floors
    // ==================================================================

    @PostMapping("/blocks/{blockId}/floors")
    public ResponseEntity<FloorResponse> createFloor(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long blockId,
            @Valid @RequestBody CreateFloorRequest request) {

        FloorResponse response =
                structureService.createFloor(authUserId, blockId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/blocks/{blockId}/floors")
    public ResponseEntity<List<FloorResponse>> getBlockFloors(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long blockId) {

        List<FloorResponse> response =
                structureService.getBlockFloors(authUserId, systemRole, blockId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/floors/{floorId}")
    public ResponseEntity<FloorResponse> getFloorById(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long floorId) {

        FloorResponse response =
                structureService.getFloorById(authUserId, systemRole, floorId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/floors/{floorId}")
    public ResponseEntity<FloorResponse> updateFloor(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long floorId,
            @Valid @RequestBody UpdateFloorRequest request) {

        FloorResponse response =
                structureService.updateFloor(authUserId, floorId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/floors/{floorId}")
    public ResponseEntity<MessageResponse> deleteFloor(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long floorId) {

        MessageResponse response =
                structureService.deleteFloor(authUserId, floorId);

        return ResponseEntity.ok(response);
    }

    // ==================================================================
    // Units
    // ==================================================================

    @PostMapping("/floors/{floorId}/units")
    public ResponseEntity<UnitResponse> createUnit(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long floorId,
            @Valid @RequestBody CreateUnitRequest request) {

        UnitResponse response =
                structureService.createUnit(authUserId, floorId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/floors/{floorId}/units")
    public ResponseEntity<List<UnitResponse>> getFloorUnits(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long floorId) {

        List<UnitResponse> response =
                structureService.getFloorUnits(authUserId, systemRole, floorId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<UnitResponse> getUnitById(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long unitId) {

        UnitResponse response =
                structureService.getUnitById(authUserId, systemRole, unitId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/units/{unitId}")
    public ResponseEntity<UnitResponse> updateUnit(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long unitId,
            @Valid @RequestBody UpdateUnitRequest request) {

        UnitResponse response =
                structureService.updateUnit(authUserId, unitId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/units/{unitId}/status")
    public ResponseEntity<UnitResponse> updateUnitStatus(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long unitId,
            @Valid @RequestBody UpdateUnitStatusRequest request) {

        UnitResponse response =
                structureService.updateUnitStatus(authUserId, unitId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/units/{unitId}")
    public ResponseEntity<MessageResponse> deleteUnit(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long unitId) {

        MessageResponse response =
                structureService.deleteUnit(authUserId, unitId);

        return ResponseEntity.ok(response);
    }
}
