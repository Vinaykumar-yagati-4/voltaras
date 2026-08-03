package com.voltaras.organizationservice.service.impl;

import com.voltaras.organizationservice.dto.request.CreateJoinRequest;
import com.voltaras.organizationservice.dto.request.RejectJoinRequest;
import com.voltaras.organizationservice.dto.response.JoinRequestResponse;
import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.entity.OrganizationJoinRequest;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.JoinRequestStatus;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.DuplicateResourceException;
import com.voltaras.organizationservice.exception.InvalidStateException;
import com.voltaras.organizationservice.exception.ResourceNotFoundException;
import com.voltaras.organizationservice.mapper.OrganizationJoinRequestMapper;
import com.voltaras.organizationservice.repository.OrganizationJoinRequestRepository;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.JoinRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements the join-request workflow.
 *
 * Approval creates an ACTIVE membership with the requested role in the same
 * transaction. A previously removed or suspended membership row is reactivated
 * instead of creating a duplicate membership.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JoinRequestServiceImpl implements JoinRequestService {

    private final OrganizationJoinRequestRepository joinRequestRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationJoinRequestMapper joinRequestMapper;
    private final OrganizationAccessHelper accessHelper;

    @Override
    @Transactional
    public JoinRequestResponse createJoinRequest(
            Long authUserId,
            Long organizationId,
            CreateJoinRequest request
    ) {

        accessHelper.requireAuthenticatedUser(authUserId);

        Organization organization =
                accessHelper.requireActiveOrganization(organizationId);

        /*
         * An ACTIVE member cannot create another join request for the same
         * organization.
         */
        if (membershipRepository
                .existsByOrganizationIdAndAuthUserIdAndMembershipStatus(
                        organizationId,
                        authUserId,
                        MembershipStatus.ACTIVE
                )) {

            log.warn(
                    "Join request rejected: user {} is already an active member of organization {}",
                    authUserId,
                    organizationId
            );

            throw new DuplicateResourceException(
                    "Membership",
                    "organizationId, authUserId",
                    organizationId + ", " + authUserId
            );
        }

        /*
         * Only one PENDING request is allowed for the same organization and
         * user.
         */
        if (joinRequestRepository
                .existsByOrganizationIdAndAuthUserIdAndStatus(
                        organizationId,
                        authUserId,
                        JoinRequestStatus.PENDING
                )) {

            log.warn(
                    "Duplicate pending join request rejected: organizationId={}, authUserId={}",
                    organizationId,
                    authUserId
            );

            throw new DuplicateResourceException(
                    "JoinRequest",
                    "organizationId, authUserId",
                    organizationId + ", " + authUserId
            );
        }

        /*
         * MEMBER is the default requested role.
         */
        MembershipRole requestedRole =
                request.getRequestedRole() == null
                        ? MembershipRole.MEMBER
                        : request.getRequestedRole();

        validateRequestedRole(requestedRole);

        OrganizationJoinRequest joinRequest =
                OrganizationJoinRequest.builder()
                        .organization(organization)
                        .authUserId(authUserId)
                        .requestedRole(requestedRole)
                        .requestMessage(request.getRequestMessage())
                        .status(JoinRequestStatus.PENDING)
                        .build();

        OrganizationJoinRequest saved =
                joinRequestRepository.save(joinRequest);

        log.info(
                "Join request created: requestId={}, organizationId={}, authUserId={}, requestedRole={}",
                saved.getId(),
                organizationId,
                authUserId,
                requestedRole
        );

        return joinRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getOrganizationJoinRequests(
            Long adminUserId,
            Long organizationId,
            JoinRequestStatus status
    ) {

        accessHelper.requireOrganizationRole(
                organizationId,
                adminUserId,
                MembershipRole.OWNER,
                MembershipRole.ORGANIZATION_ADMIN
        );

        List<OrganizationJoinRequest> requests;

        if (status == null) {

            requests =
                    joinRequestRepository
                            .findAllByOrganizationIdOrderByCreatedAtDesc(
                                    organizationId
                            );

        } else {

            requests =
                    joinRequestRepository
                            .findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(
                                    organizationId,
                                    status
                            );
        }

        return requests.stream()
                .map(joinRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getMyJoinRequests(Long authUserId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        return joinRequestRepository
                .findAllByAuthUserIdOrderByCreatedAtDesc(authUserId)
                .stream()
                .map(joinRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public JoinRequestResponse approveJoinRequest(
            Long reviewerUserId,
            Long organizationId,
            Long requestId
    ) {

        accessHelper.requireOrganizationRole(
                organizationId,
                reviewerUserId,
                MembershipRole.OWNER,
                MembershipRole.ORGANIZATION_ADMIN
        );

        OrganizationJoinRequest joinRequest =
                findPendingRequest(organizationId, requestId);

        /*
         * Create a new membership or reactivate an existing suspended/removed
         * membership.
         */
        createOrReactivateMembership(joinRequest);

        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setReviewedByAuthUserId(reviewerUserId);
        joinRequest.setReviewedAt(LocalDateTime.now());

        OrganizationJoinRequest saved =
                joinRequestRepository.save(joinRequest);

        log.info(
                "Join request approved: requestId={}, organizationId={}, authUserId={}, reviewerUserId={}",
                saved.getId(),
                organizationId,
                saved.getAuthUserId(),
                reviewerUserId
        );

        return joinRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public JoinRequestResponse rejectJoinRequest(
            Long reviewerUserId,
            Long organizationId,
            Long requestId,
            RejectJoinRequest request
    ) {

        /*
         * TEMPORARY DEBUG LOGS
         *
         * These logs help us verify which values are actually received from
         * the API Gateway and controller.
         */
        log.info("========== REJECT JOIN REQUEST DEBUG ==========");
        log.info("Reviewer User ID : {}", reviewerUserId);
        log.info("Organization ID  : {}", organizationId);
        log.info("Join Request ID  : {}", requestId);
        log.info("Rejection Remarks: {}", request.getRemarks());
        log.info("================================================");

        /*
         * Only an ACTIVE OWNER or ORGANIZATION_ADMIN of the organization can
         * reject a join request.
         */
        accessHelper.requireOrganizationRole(
                organizationId,
                reviewerUserId,
                MembershipRole.OWNER,
                MembershipRole.ORGANIZATION_ADMIN
        );

        /*
         * Remarks are mandatory.
         * This is also validated by @NotBlank inside RejectJoinRequest.
         */
        if (!StringUtils.hasText(request.getRemarks())) {
            throw new BadRequestException(
                    "Rejection remarks are required"
            );
        }

        /*
         * Only a PENDING join request can be rejected.
         */
        OrganizationJoinRequest joinRequest =
                findPendingRequest(organizationId, requestId);

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setRejectionRemarks(request.getRemarks());
        joinRequest.setReviewedByAuthUserId(reviewerUserId);
        joinRequest.setReviewedAt(LocalDateTime.now());

        OrganizationJoinRequest saved =
                joinRequestRepository.save(joinRequest);

        log.info(
                "Join request rejected: requestId={}, organizationId={}, authUserId={}, reviewerUserId={}",
                saved.getId(),
                organizationId,
                saved.getAuthUserId(),
                reviewerUserId
        );

        return joinRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public JoinRequestResponse cancelJoinRequest(
            Long authUserId,
            Long organizationId,
            Long requestId
    ) {

        OrganizationJoinRequest joinRequest =
                joinRequestRepository
                        .findByIdAndOrganizationId(
                                requestId,
                                organizationId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "JoinRequest",
                                        "id",
                                        requestId
                                )
                        );

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {

            throw new InvalidStateException(
                    "Only PENDING join requests can be cancelled"
            );
        }

        /*
         * The original requester can cancel their own request.
         *
         * Otherwise, an OWNER or ORGANIZATION_ADMIN can cancel the pending
         * request.
         */
        boolean isRequester =
                joinRequest.getAuthUserId().equals(authUserId);

        if (!isRequester) {

            accessHelper.requireOrganizationRole(
                    organizationId,
                    authUserId,
                    MembershipRole.OWNER,
                    MembershipRole.ORGANIZATION_ADMIN
            );
        }

        joinRequest.setStatus(JoinRequestStatus.CANCELLED);

        OrganizationJoinRequest saved =
                joinRequestRepository.save(joinRequest);

        log.info(
                "Join request cancelled: requestId={}, organizationId={}, authUserId={}",
                saved.getId(),
                organizationId,
                authUserId
        );

        return joinRequestMapper.toResponse(saved);
    }

    /*
     * Finds a join request inside the requested organization and confirms that
     * its current status is PENDING.
     */
    private OrganizationJoinRequest findPendingRequest(
            Long organizationId,
            Long requestId
    ) {

        OrganizationJoinRequest joinRequest =
                joinRequestRepository
                        .findByIdAndOrganizationId(
                                requestId,
                                organizationId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "JoinRequest",
                                        "id",
                                        requestId
                                )
                        );

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {

            throw new InvalidStateException(
                    "Only PENDING join requests can be reviewed"
            );
        }

        return joinRequest;
    }

    /**
     * Creates a new ACTIVE membership or reactivates a previously removed or
     * suspended membership.
     *
     * @param joinRequest approved join request
     */
    private void createOrReactivateMembership(
            OrganizationJoinRequest joinRequest
    ) {

        Long organizationId =
                joinRequest.getOrganization().getId();

        Long authUserId =
                joinRequest.getAuthUserId();

        OrganizationMembership existing =
                membershipRepository
                        .findByOrganizationIdAndAuthUserId(
                                organizationId,
                                authUserId
                        )
                        .orElse(null);

        if (existing != null
                && existing.getMembershipStatus()
                == MembershipStatus.ACTIVE) {

            log.warn(
                    "Approval rejected: user {} is already an active member of organization {}",
                    authUserId,
                    organizationId
            );

            throw new DuplicateResourceException(
                    "Membership",
                    "organizationId, authUserId",
                    organizationId + ", " + authUserId
            );
        }

        if (existing != null) {

            /*
             * Reactivate a previously suspended or removed membership.
             */
            existing.setMembershipStatus(
                    MembershipStatus.ACTIVE
            );

            existing.setMembershipRole(
                    joinRequest.getRequestedRole()
            );

            existing.setJoinedAt(
                    LocalDateTime.now()
            );

            membershipRepository.save(existing);

            return;
        }

        OrganizationMembership membership =
                OrganizationMembership.builder()
                        .organization(joinRequest.getOrganization())
                        .authUserId(authUserId)
                        .membershipRole(joinRequest.getRequestedRole())
                        .membershipStatus(MembershipStatus.ACTIVE)
                        .build();

        membershipRepository.save(membership);
    }

    /*
     * OWNER and ORGANIZATION_ADMIN cannot be requested through a join request.
     */
    private void validateRequestedRole(
            MembershipRole requestedRole
    ) {

        if (requestedRole == MembershipRole.OWNER
                || requestedRole
                == MembershipRole.ORGANIZATION_ADMIN) {

            throw new BadRequestException(
                    "requestedRole cannot be OWNER or ORGANIZATION_ADMIN"
            );
        }
    }
}