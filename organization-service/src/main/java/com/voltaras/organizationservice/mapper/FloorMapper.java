package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.request.CreateFloorRequest;
import com.voltaras.organizationservice.dto.request.UpdateFloorRequest;
import com.voltaras.organizationservice.dto.response.FloorResponse;
import com.voltaras.organizationservice.entity.Floor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Floor entities to/from request and response DTOs. floorNumber is
 * immutable after creation and is never mapped on update.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FloorMapper {

    Floor toEntity(CreateFloorRequest request);

    /**
     * floorNumber is immutable after creation and is never applied on update.
     */
    @Mapping(target = "floorNumber", ignore = true)
    void updateEntity(UpdateFloorRequest request, @MappingTarget Floor floor);

    @Mapping(
            target = "blockId",
            expression = "java(floor.getBlock() != null ? floor.getBlock().getId() : null)"
    )
    @Mapping(
            target = "blockName",
            expression = "java(floor.getBlock() != null ? floor.getBlock().getName() : null)"
    )
    FloorResponse toResponse(Floor floor);
}
