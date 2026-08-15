package com.voltaras.organizationservice.mapper;

import com.voltaras.organizationservice.dto.request.CreateOrganizationRequest;
import com.voltaras.organizationservice.dto.request.UpdateOrganizationRequest;
import com.voltaras.organizationservice.dto.response.AvailableOrganizationResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Maps Organization entities to/from request and response DTOs.
 * System-controlled fields (id, createdByAuthUserId, status, createdAt,
 * updatedAt) are assigned by the service layer, never by the mapper.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

    Organization toEntity(CreateOrganizationRequest request);

    /**
     * Full update (PUT): overwrites all updatable fields.
     * organizationCode and createdByAuthUserId are never mapped.
     */
    void updateEntity(UpdateOrganizationRequest request, @MappingTarget Organization organization);

    OrganizationResponse toResponse(Organization organization);

    /**
     * Maps to the lightweight view used by consumers browsing organizations
     * they can request access to. Contact details are not mapped.
     */
    AvailableOrganizationResponse toAvailableResponse(Organization organization);
}
