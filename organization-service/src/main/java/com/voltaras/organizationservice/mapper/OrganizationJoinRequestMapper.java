package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.response.JoinRequestResponse;
import com.voltaras.organizationservice.entity.OrganizationJoinRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps OrganizationJoinRequest entities to response DTOs. The nested
 * organization id/name are extracted explicitly so lazy entities are never
 * exposed to clients.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationJoinRequestMapper {

    @Mapping(
            target = "organizationId",
            expression = "java(request.getOrganization() != null ? request.getOrganization().getId() : null)"
    )
    @Mapping(
            target = "organizationName",
            expression = "java(request.getOrganization() != null ? request.getOrganization().getName() : null)"
    )
    JoinRequestResponse toResponse(OrganizationJoinRequest request);
}
