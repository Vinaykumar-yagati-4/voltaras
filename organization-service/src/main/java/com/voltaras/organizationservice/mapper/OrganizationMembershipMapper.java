package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps OrganizationMembership entities to response DTOs. The nested
 * organization id/name are extracted explicitly so lazy entities are never
 * exposed to clients.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMembershipMapper {

    @Mapping(
            target = "organizationId",
            expression = "java(membership.getOrganization() != null ? membership.getOrganization().getId() : null)"
    )
    @Mapping(
            target = "organizationName",
            expression = "java(membership.getOrganization() != null ? membership.getOrganization().getName() : null)"
    )
    MembershipResponse toResponse(OrganizationMembership membership);
}
