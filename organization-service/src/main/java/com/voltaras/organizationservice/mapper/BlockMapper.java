package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.request.CreateBlockRequest;
import com.voltaras.organizationservice.dto.request.UpdateBlockRequest;
import com.voltaras.organizationservice.dto.response.BlockResponse;
import com.voltaras.organizationservice.entity.Block;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BlockMapper {

    Block toEntity(CreateBlockRequest request);

    void updateEntity(UpdateBlockRequest request, @MappingTarget Block block);

    @Mapping(
            target = "buildingId",
            expression = "java(block.getBuilding() != null ? block.getBuilding().getId() : null)"
    )
    @Mapping(
            target = "buildingName",
            expression = "java(block.getBuilding() != null ? block.getBuilding().getName() : null)"
    )
    BlockResponse toResponse(Block block);
}
