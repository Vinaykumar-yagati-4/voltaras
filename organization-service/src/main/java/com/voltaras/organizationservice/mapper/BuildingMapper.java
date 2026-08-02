package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.request.CreateBuildingRequest;
import com.voltaras.organizationservice.dto.request.UpdateBuildingRequest;
import com.voltaras.organizationservice.dto.response.BuildingResponse;
import com.voltaras.organizationservice.entity.Building;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BuildingMapper {

    Building toEntity(CreateBuildingRequest request);

    void updateEntity(UpdateBuildingRequest request, @MappingTarget Building building);

    @Mapping(
            target = "organizationId",
            expression = "java(building.getOrganization() != null ? building.getOrganization().getId() : null)"
    )
    BuildingResponse toResponse(Building building);
}
