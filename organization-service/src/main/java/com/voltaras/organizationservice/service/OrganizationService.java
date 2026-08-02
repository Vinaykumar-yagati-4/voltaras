package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.CreateOrganizationRequest;
import com.voltaras.organizationservice.dto.request.UpdateOrganizationRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Business contract for organization management.
 * <p>
 * Authenticated identity (authUserId) and platform role (systemRole) are
 * always supplied by the caller — read from the gateway-injected
 * {@code X-User-Id} / {@code X-User-Role} headers, never parsed from the
 * request body or URL.
 */
public interface OrganizationService {

    /**
     * Creates an organization and automatically creates the ACTIVE OWNER
     * membership for the creator in one transaction.
     */
    OrganizationResponse createOrganization(Long authUserId, CreateOrganizationRequest request);

    /**
     * Returns the caller's organizations where the membership is ACTIVE,
     * including the membership role and status.
     */
    List<MembershipResponse> getMyOrganizations(Long authUserId);

    /**
     * Views an organization. Allowed for ACTIVE members and system ADMIN.
     */
    OrganizationResponse getOrganizationById(
            Long authUserId, String systemRole, Long organizationId);

    /**
     * Full update (PUT). Allowed for OWNER and ORGANIZATION_ADMIN while the
     * organization is ACTIVE.
     */
    OrganizationResponse updateOrganization(
            Long authUserId, Long organizationId, UpdateOrganizationRequest request);

    /**
     * Deactivates an organization (status -> INACTIVE). Allowed for the
     * organization OWNER and system ADMIN.
     */
    OrganizationResponse deactivateOrganization(
            Long authUserId, String systemRole, Long organizationId);

    /**
     * System ADMIN: paginated list of all organizations, optionally filtered
     * by status and/or type.
     */
    Page<OrganizationResponse> getAllOrganizationsForAdmin(
            String systemRole, OrganizationStatus status, OrganizationType type, Pageable pageable);

    /**
     * System ADMIN: views any organization.
     */
    OrganizationResponse getOrganizationForAdmin(String systemRole, Long organizationId);

    /**
     * System ADMIN: suspends an organization (status -> SUSPENDED).
     */
    OrganizationResponse suspendOrganizationForAdmin(String systemRole, Long organizationId);

    /**
     * System ADMIN: activates an organization (status -> ACTIVE).
     */
    OrganizationResponse activateOrganizationForAdmin(String systemRole, Long organizationId);
}
