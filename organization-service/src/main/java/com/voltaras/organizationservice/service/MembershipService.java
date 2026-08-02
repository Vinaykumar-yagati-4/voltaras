package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.UpdateMembershipRoleRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Business contract for organization membership management.
 * <p>
 * Authenticated identity is always supplied by the caller from the
 * gateway-injected {@code X-User-Id} header.
 */
public interface MembershipService {

    /**
     * Paginated member list. Allowed for OWNER, ORGANIZATION_ADMIN and
     * MANAGER.
     */
    Page<MembershipResponse> getOrganizationMembers(
            Long requesterUserId, Long organizationId, Pageable pageable);

    /**
     * Changes a member's role. Only the OWNER may assign or remove the
     * ORGANIZATION_ADMIN role; the OWNER role itself can never be assigned
     * or changed.
     */
    MembershipResponse updateMembershipRole(
            Long requesterUserId, Long organizationId, Long membershipId,
            UpdateMembershipRoleRequest request);

    /**
     * Suspends an ACTIVE member (status -> SUSPENDED). Allowed for OWNER
     * and ORGANIZATION_ADMIN. The OWNER can never be suspended.
     */
    MembershipResponse suspendMember(
            Long requesterUserId, Long organizationId, Long membershipId);

    /**
     * Soft-removes a member (status -> REMOVED) and returns a confirmation
     * message. Allowed for OWNER and ORGANIZATION_ADMIN.
     */
    MessageResponse removeMember(
            Long requesterUserId, Long organizationId, Long membershipId);
}
