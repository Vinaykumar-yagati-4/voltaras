package com.voltaras.paymentservice.client;

/**
 * Client for the VOLTARAS Organization Service used to authorize
 * organization access.
 */
public interface OrganizationServiceClient {

    /**
     * Verifies that the caller may access the organization: either the
     * caller is a system ADMIN or has an ACTIVE membership in the
     * organization. Throws the corresponding VOLTARAS exception when the
     * organization does not exist or access is denied.
     *
     * @param organizationId organization ID
     * @param authUserId authenticated user ID
     * @param systemRole platform role from X-User-Role
     */
    void requireOrganizationAccess(
            Long organizationId, Long authUserId, String systemRole);
}
