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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Reusable authorization and identity helpers shared by all service
 * implementations.
 *
 * <p>
 * Identity comes from API Gateway headers:
 * X-User-Id and X-User-Role.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationAccessHelper {

    private static final String SYSTEM_ADMIN_ROLE = "ADMIN";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;

    /**
     * Requires a valid authenticated user ID.
     *
     * @param authUserId authenticated user ID from X-User-Id
     */
    public void requireAuthenticatedUser(Long authUserId) {

        if (authUserId == null) {
            throw new BadRequestException(
                    "Authenticated user is required"
            );
        }
    }

    /**
     * Checks whether the provided platform role is a system ADMIN role.
     *
     * @param systemRole role received from X-User-Role
     * @return true when role is ADMIN or ROLE_ADMIN
     */
    public boolean isSystemAdmin(String systemRole) {

        if (systemRole == null) {
            return false;
        }

        String normalized =
                systemRole.trim().toUpperCase();

        if (normalized.startsWith("ROLE_")) {
            normalized =
                    normalized.substring("ROLE_".length());
        }

        return SYSTEM_ADMIN_ROLE.equals(normalized);
    }

    /**
     * Requires the caller to be a system ADMIN.
     *
     * @param systemRole role received from API Gateway
     */
    public void requireSystemAdmin(String systemRole) {

        if (!isSystemAdmin(systemRole)) {
            throw new ForbiddenOperationException(
                    "Only system ADMIN users can perform this operation"
            );
        }
    }

    /**
     * Loads an organization by ID.
     *
     * @param organizationId organization ID
     * @return organization entity
     */
    public Organization requireOrganization(Long organizationId) {

        return organizationRepository
                .findById(organizationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Organization",
                                "id",
                                organizationId
                        )
                );
    }

    /**
     * Loads an organization and verifies that it is ACTIVE.
     *
     * @param organizationId organization ID
     * @return active organization
     */
    public Organization requireActiveOrganization(Long organizationId) {

        Organization organization =
                requireOrganization(organizationId);

        requireOrganizationActive(organization);

        return organization;
    }

    /**
     * Requires an organization to have ACTIVE status.
     *
     * @param organization organization entity
     */
    public void requireOrganizationActive(
            Organization organization
    ) {

        if (organization.getStatus()
                != OrganizationStatus.ACTIVE) {

            throw new BadRequestException(
                    "Organization is not active"
            );
        }
    }

    /**
     * Finds the caller's ACTIVE membership.
     *
     * <p>
     * Temporary logs are included to verify the organization ID and
     * authenticated user ID received through the Gateway.
     * </p>
     *
     * @param organizationId organization ID
     * @param authUserId authenticated user ID
     * @return active membership
     */
    public OrganizationMembership getActiveMembership(
            Long organizationId,
            Long authUserId
    ) {

        requireAuthenticatedUser(authUserId);

        log.info("========== MEMBERSHIP CHECK ==========");
        log.info("Organization ID : {}", organizationId);
        log.info("Auth User ID    : {}", authUserId);

        OrganizationMembership membership =
                membershipRepository
                        .findByOrganizationIdAndAuthUserId(
                                organizationId,
                                authUserId
                        )
                        .orElse(null);

        if (membership == null) {

            log.warn("Membership      : NOT FOUND");
            log.info("======================================");

            throw new ForbiddenOperationException(
                    "You are not an active member of this organization"
            );
        }

        log.info(
                "Membership ID     : {}",
                membership.getId()
        );

        log.info(
                "Membership Role   : {}",
                membership.getMembershipRole()
        );

        log.info(
                "Membership Status : {}",
                membership.getMembershipStatus()
        );

        log.info("======================================");

        if (membership.getMembershipStatus()
                != MembershipStatus.ACTIVE) {

            throw new ForbiddenOperationException(
                    "You are not an active member of this organization"
            );
        }

        return membership;
    }

    /**
     * Requires the ACTIVE membership to contain one of the allowed
     * organization roles.
     *
     * @param organizationId organization ID
     * @param authUserId authenticated user ID
     * @param allowedRoles accepted organization roles
     * @return valid active membership
     */
    public OrganizationMembership requireOrganizationRole(
            Long organizationId,
            Long authUserId,
            MembershipRole... allowedRoles
    ) {

        OrganizationMembership membership =
                getActiveMembership(
                        organizationId,
                        authUserId
                );

        log.info(
                "Allowed roles      : {}",
                Arrays.toString(allowedRoles)
        );

        log.info(
                "Actual role        : {}",
                membership.getMembershipRole()
        );

        boolean permitted =
                Arrays.stream(allowedRoles)
                        .anyMatch(
                                role ->
                                        role
                                                == membership
                                                .getMembershipRole()
                        );

        log.info(
                "Role permitted     : {}",
                permitted
        );

        if (!permitted) {

            throw new ForbiddenOperationException(
                    "Insufficient organization role for this operation"
            );
        }

        return membership;
    }
}