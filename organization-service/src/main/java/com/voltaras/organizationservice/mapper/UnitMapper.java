package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.request.CreateUnitRequest;
import com.voltaras.organizationservice.dto.request.UpdateUnitRequest;
import com.voltaras.organizationservice.dto.response.UnitResponse;
import com.voltaras.organizationservice.entity.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Unit entities to/from request and response DTOs. The unit number is
 * immutable after creation and is never mapped on update.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UnitMapper {

    Unit toEntity(CreateUnitRequest request);

    void updateEntity(UpdateUnitRequest request, @MappingTarget Unit unit);

    @Mapping(
            target = "floorId",
            expression = "java(unit.getFloor() != null ? unit.getFloor().getId() : null)"
    )
    @Mapping(
            target = "floorNumber",
            expression = "java(unit.getFloor() != null ? unit.getFloor().getFloorNumber() : null)"
    )
    UnitResponse toResponse(Unit unit);
}
