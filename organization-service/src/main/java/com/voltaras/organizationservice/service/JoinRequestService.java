package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.CreateJoinRequest;
import com.voltaras.organizationservice.dto.request.RejectJoinRequest;
import com.voltaras.organizationservice.dto.response.JoinRequestResponse;
import com.voltaras.organizationservice.enums.JoinRequestStatus;

import java.util.List;

/**
 * Business contract for organization join-request management.
 * <p>
 * Authenticated identity is always supplied by the caller from the
 * gateway-injected {@code X-User-Id} header.
 */
public interface JoinRequestService {

    /**
     * Creates a PENDING join request for a non-member. The requested role
     * defaults to MEMBER and may never be OWNER or ORGANIZATION_ADMIN.
     */
    JoinRequestResponse createJoinRequest(
            Long authUserId, Long organizationId, CreateJoinRequest request);

    /**
     * Lists join requests of an organization, optionally filtered by status.
     * Allowed for OWNER and ORGANIZATION_ADMIN.
     */
    List<JoinRequestResponse> getOrganizationJoinRequests(
            Long adminUserId, Long organizationId, JoinRequestStatus status);

    /**
     * Lists the caller's own join requests.
     */
    List<JoinRequestResponse> getMyJoinRequests(Long authUserId);

    /**
     * Approves a PENDING request, creating an ACTIVE membership with the
     * requested role in the same transaction. Allowed for OWNER and
     * ORGANIZATION_ADMIN.
     */
    JoinRequestResponse approveJoinRequest(
            Long reviewerUserId, Long organizationId, Long requestId);

    /**
     * Rejects a PENDING request with mandatory remarks. Allowed for OWNER
     * and ORGANIZATION_ADMIN.
     */
    JoinRequestResponse rejectJoinRequest(
            Long reviewerUserId, Long organizationId, Long requestId,
            RejectJoinRequest request);

    /**
     * Cancels a PENDING request. The original requester, OWNER and
     * ORGANIZATION_ADMIN may cancel.
     */
    JoinRequestResponse cancelJoinRequest(
            Long authUserId, Long organizationId, Long requestId);
}
