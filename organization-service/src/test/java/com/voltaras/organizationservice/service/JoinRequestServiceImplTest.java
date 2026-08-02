package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.CreateJoinRequest;
import com.voltaras.organizationservice.dto.request.RejectJoinRequest;
import com.voltaras.organizationservice.dto.response.JoinRequestResponse;
import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.entity.OrganizationJoinRequest;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.JoinRequestStatus;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.DuplicateResourceException;
import com.voltaras.organizationservice.exception.ForbiddenOperationException;
import com.voltaras.organizationservice.exception.InvalidStateException;
import com.voltaras.organizationservice.mapper.OrganizationJoinRequestMapper;
import com.voltaras.organizationservice.repository.OrganizationJoinRequestRepository;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.impl.JoinRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JoinRequestServiceImpl} covering the join-request
 * workflow: create, duplicate prevention, approve, reject and cancel.
 */
@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    private static final Long REVIEWER_ID = 1L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long ORG_ID = 1L;
    private static final Long REQUEST_ID = 5L;

    @Mock private OrganizationJoinRequestRepository joinRequestRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private OrganizationJoinRequestMapper joinRequestMapper;
    @Mock private OrganizationAccessHelper accessHelper;

    private JoinRequestServiceImpl joinRequestService;

    @BeforeEach
    void setUp() {
        joinRequestService = new JoinRequestServiceImpl(
                joinRequestRepository, membershipRepository,
                joinRequestMapper, accessHelper);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create: non-member request starts PENDING with requested role")
    void createJoinRequest_success_pending() {

        CreateJoinRequest request = CreateJoinRequest.builder()
                .requestedRole(MembershipRole.TENANT)
                .requestMessage("I would like to join")
                .build();

        Organization organization = buildOrganization();

        when(accessHelper.requireActiveOrganization(ORG_ID)).thenReturn(organization);
        when(membershipRepository.existsByOrganizationIdAndAuthUserIdAndMembershipStatus(
                ORG_ID, REQUESTER_ID, MembershipStatus.ACTIVE)).thenReturn(false);
        when(joinRequestRepository.existsByOrganizationIdAndAuthUserIdAndStatus(
                ORG_ID, REQUESTER_ID, JoinRequestStatus.PENDING)).thenReturn(false);

        when(joinRequestRepository.save(any(OrganizationJoinRequest.class)))
                .thenAnswer(invocation -> {
                    OrganizationJoinRequest saved = invocation.getArgument(0);
                    saved.setId(REQUEST_ID);
                    return saved;
                });
        when(joinRequestMapper.toResponse(any())).thenReturn(JoinRequestResponse.builder()
                .id(REQUEST_ID).status(JoinRequestStatus.PENDING).build());

        JoinRequestResponse response =
                joinRequestService.createJoinRequest(REQUESTER_ID, ORG_ID, request);

        assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.PENDING);

        verify(joinRequestRepository).save(argThat(jr ->
                jr.getAuthUserId().equals(REQUESTER_ID)
                        && jr.getRequestedRole() == MembershipRole.TENANT
                        && jr.getStatus() == JoinRequestStatus.PENDING));
    }

    @Test
    @DisplayName("Create: duplicate PENDING request rejected")
    void createJoinRequest_duplicatePending_throwsDuplicate() {

        CreateJoinRequest request = CreateJoinRequest.builder().build();

        when(accessHelper.requireActiveOrganization(ORG_ID)).thenReturn(buildOrganization());
        when(membershipRepository.existsByOrganizationIdAndAuthUserIdAndMembershipStatus(
                ORG_ID, REQUESTER_ID, MembershipStatus.ACTIVE)).thenReturn(false);
        when(joinRequestRepository.existsByOrganizationIdAndAuthUserIdAndStatus(
                ORG_ID, REQUESTER_ID, JoinRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() ->
                joinRequestService.createJoinRequest(REQUESTER_ID, ORG_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create: existing ACTIVE member cannot request")
    void createJoinRequest_existingMember_throwsDuplicate() {

        CreateJoinRequest request = CreateJoinRequest.builder().build();

        when(accessHelper.requireActiveOrganization(ORG_ID)).thenReturn(buildOrganization());
        when(membershipRepository.existsByOrganizationIdAndAuthUserIdAndMembershipStatus(
                ORG_ID, REQUESTER_ID, MembershipStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() ->
                joinRequestService.createJoinRequest(REQUESTER_ID, ORG_ID, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create: OWNER role cannot be requested")
    void createJoinRequest_ownerRoleRequested_throwsBadRequest() {

        CreateJoinRequest request = CreateJoinRequest.builder()
                .requestedRole(MembershipRole.OWNER)
                .build();

        when(accessHelper.requireActiveOrganization(ORG_ID)).thenReturn(buildOrganization());
        when(membershipRepository.existsByOrganizationIdAndAuthUserIdAndMembershipStatus(
                ORG_ID, REQUESTER_ID, MembershipStatus.ACTIVE)).thenReturn(false);
        when(joinRequestRepository.existsByOrganizationIdAndAuthUserIdAndStatus(
                ORG_ID, REQUESTER_ID, JoinRequestStatus.PENDING)).thenReturn(false);

        assertThatThrownBy(() ->
                joinRequestService.createJoinRequest(REQUESTER_ID, ORG_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be OWNER");
    }

    // ------------------------------------------------------------------
    // Approve
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Approve: creates ACTIVE membership with requested role")
    void approveJoinRequest_success_createsMembership() {

        OrganizationJoinRequest joinRequest = buildPendingRequest();

        when(accessHelper.requireOrganizationRole(
                ORG_ID, REVIEWER_ID, MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN))
                .thenReturn(OrganizationMembership.builder()
                        .membershipRole(MembershipRole.OWNER).build());
        when(joinRequestRepository.findByIdAndOrganizationId(REQUEST_ID, ORG_ID))
                .thenReturn(Optional.of(joinRequest));
        when(membershipRepository.findByOrganizationIdAndAuthUserId(ORG_ID, REQUESTER_ID))
                .thenReturn(Optional.empty());
        when(joinRequestRepository.save(joinRequest)).thenReturn(joinRequest);
        when(joinRequestMapper.toResponse(joinRequest)).thenReturn(JoinRequestResponse.builder()
                .id(REQUEST_ID).status(JoinRequestStatus.APPROVED).build());

        JoinRequestResponse response =
                joinRequestService.approveJoinRequest(REVIEWER_ID, ORG_ID, REQUEST_ID);

        assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
        assertThat(joinRequest.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
        assertThat(joinRequest.getReviewedByAuthUserId()).isEqualTo(REVIEWER_ID);
        assertThat(joinRequest.getReviewedAt()).isNotNull();

        verify(membershipRepository).save(argThat(m ->
                m.getAuthUserId().equals(REQUESTER_ID)
                        && m.getMembershipRole() == MembershipRole.TENANT
                        && m.getMembershipStatus() == MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Approve: non-PENDING request is rejected")
    void approveJoinRequest_nonPending_throwsInvalidState() {

        OrganizationJoinRequest rejected = buildPendingRequest();
        rejected.setStatus(JoinRequestStatus.REJECTED);

        when(joinRequestRepository.findByIdAndOrganizationId(REQUEST_ID, ORG_ID))
                .thenReturn(Optional.of(rejected));

        assertThatThrownBy(() ->
                joinRequestService.approveJoinRequest(REVIEWER_ID, ORG_ID, REQUEST_ID))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("PENDING");

        verify(membershipRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Reject
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Reject: remarks are mandatory")
    void rejectJoinRequest_blankRemarks_throwsBadRequest() {

        RejectJoinRequest request = RejectJoinRequest.builder().remarks("   ").build();

        when(accessHelper.requireOrganizationRole(
                ORG_ID, REVIEWER_ID, MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN))
                .thenReturn(OrganizationMembership.builder()
                        .membershipRole(MembershipRole.ORGANIZATION_ADMIN).build());

        assertThatThrownBy(() ->
                joinRequestService.rejectJoinRequest(REVIEWER_ID, ORG_ID, REQUEST_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("remarks");

        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject: stores remarks, reviewer and reviewedAt")
    void rejectJoinRequest_success() {

        RejectJoinRequest request = RejectJoinRequest.builder()
                .remarks("Not enough capacity").build();

        OrganizationJoinRequest joinRequest = buildPendingRequest();

        when(accessHelper.requireOrganizationRole(
                ORG_ID, REVIEWER_ID, MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN))
                .thenReturn(OrganizationMembership.builder()
                        .membershipRole(MembershipRole.ORGANIZATION_ADMIN).build());
        when(joinRequestRepository.findByIdAndOrganizationId(REQUEST_ID, ORG_ID))
                .thenReturn(Optional.of(joinRequest));
        when(joinRequestRepository.save(joinRequest)).thenReturn(joinRequest);
        when(joinRequestMapper.toResponse(joinRequest)).thenReturn(JoinRequestResponse.builder()
                .id(REQUEST_ID).status(JoinRequestStatus.REJECTED).build());

        JoinRequestResponse response =
                joinRequestService.rejectJoinRequest(REVIEWER_ID, ORG_ID, REQUEST_ID, request);

        assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
        assertThat(joinRequest.getRejectionRemarks()).isEqualTo("Not enough capacity");
        assertThat(joinRequest.getReviewedByAuthUserId()).isEqualTo(REVIEWER_ID);
        assertThat(joinRequest.getReviewedAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // Cancel
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Cancel: another non-admin user cannot cancel")
    void cancelJoinRequest_otherUser_throwsForbidden() {

        OrganizationJoinRequest joinRequest = buildPendingRequest();

        when(joinRequestRepository.findByIdAndOrganizationId(REQUEST_ID, ORG_ID))
                .thenReturn(Optional.of(joinRequest));
        doThrow(new ForbiddenOperationException("Insufficient organization role"))
                .when(accessHelper).requireOrganizationRole(
                        ORG_ID, 999L,
                        MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);

        assertThatThrownBy(() ->
                joinRequestService.cancelJoinRequest(999L, ORG_ID, REQUEST_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Cancel: original requester can cancel their own PENDING request")
    void cancelJoinRequest_requester_success() {

        OrganizationJoinRequest joinRequest = buildPendingRequest();

        when(joinRequestRepository.findByIdAndOrganizationId(REQUEST_ID, ORG_ID))
                .thenReturn(Optional.of(joinRequest));
        when(joinRequestRepository.save(joinRequest)).thenReturn(joinRequest);
        when(joinRequestMapper.toResponse(joinRequest)).thenReturn(JoinRequestResponse.builder()
                .id(REQUEST_ID).status(JoinRequestStatus.CANCELLED).build());

        JoinRequestResponse response =
                joinRequestService.cancelJoinRequest(REQUESTER_ID, ORG_ID, REQUEST_ID);

        assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.CANCELLED);
        verify(accessHelper, never()).requireOrganizationRole(
                ORG_ID, REQUESTER_ID,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Organization buildOrganization() {
        return Organization.builder()
                .id(ORG_ID)
                .name("Sunrise Hostel")
                .organizationCode("SUNRISE-HST")
                .organizationType(OrganizationType.HOSTEL)
                .status(OrganizationStatus.ACTIVE)
                .build();
    }

    private OrganizationJoinRequest buildPendingRequest() {
        return OrganizationJoinRequest.builder()
                .id(REQUEST_ID)
                .organization(buildOrganization())
                .authUserId(REQUESTER_ID)
                .requestedRole(MembershipRole.TENANT)
                .status(JoinRequestStatus.PENDING)
                .build();
    }
}
