package com.voltaras.organizationservice.security;

import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.ForbiddenOperationException;
import com.voltaras.organizationservice.exception.ResourceNotFoundException;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Reusable authorization and identity helpers shared by all service
 * implementations.
 * <p>
 * Identity always comes from the API Gateway headers (X-User-Id /
 * X-User-Role). The service never parses or validates JWTs. A request that
 * reaches this service without going through the Gateway is trusted only
 * for local debugging.
 */
@Component
@RequiredArgsConstructor
public class OrganizationAccessHelper {

    private static final String SYSTEM_ADMIN_ROLE = "ADMIN";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;

    /**
     * The authenticated user id must be present (X-User-Id header).
     */
    public void requireAuthenticatedUser(Long authUserId) {
        if (authUserId == null) {
            throw new BadRequestException("Authenticated user is required");
        }
    }

    /**
     * Normalizes the platform role so {@code ADMIN} and {@code ROLE_ADMIN}
     * are treated identically (the API Gateway may send either form).
     * ORGANIZATION_ADMIN is a membership role and is intentionally NOT
     * treated as a system admin role here.
     */
    public boolean isSystemAdmin(String systemRole) {
        if (systemRole == null) {
            return false;
        }
        String normalized = systemRole.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return SYSTEM_ADMIN_ROLE.equals(normalized);
    }

    /**
     * Only a system ADMIN (from X-User-Role) may call admin endpoints.
     */
    public void requireSystemAdmin(String systemRole) {
        if (!isSystemAdmin(systemRole)) {
            throw new ForbiddenOperationException(
                    "Only system ADMIN users can perform this operation");
        }
    }

    /**
     * Loads an organization or throws 404.
     */
    public Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization", "id", organizationId));
    }

    /**
     * Loads an organization and requires it to be ACTIVE. Inactive or
     * suspended organizations reject restricted operations.
     */
    public Organization requireActiveOrganization(Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        requireOrganizationActive(organization);
        return organization;
    }

    /**
     * Restricted operations are rejected when the organization is not ACTIVE.
     */
    public void requireOrganizationActive(Organization organization) {
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BadRequestException("Organization is not active");
        }
    }

    /**
     * Returns the caller's ACTIVE membership or throws 403. Suspended and
     * removed members are treated as non-members.
     */
    public OrganizationMembership getActiveMembership(
            Long organizationId, Long authUserId) {

        requireAuthenticatedUser(authUserId);

        return membershipRepository
                .findByOrganizationIdAndAuthUserId(organizationId, authUserId)
                .filter(membership ->
                        membership.getMembershipStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "You are not an active member of this organization"));
    }

    /**
     * Requires the caller's ACTIVE membership to hold one of the allowed
     * organization roles.
     */
    public OrganizationMembership requireOrganizationRole(
            Long organizationId, Long authUserId, MembershipRole... allowedRoles) {

        OrganizationMembership membership =
                getActiveMembership(organizationId, authUserId);

        boolean permitted = Arrays.stream(allowedRoles)
                .anyMatch(role -> role == membership.getMembershipRole());

        if (!permitted) {
            throw new ForbiddenOperationException(
                    "Insufficient organization role for this operation");
        }

        return membership;
    }
}
