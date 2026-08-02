package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.CreateOrganizationRequest;
import com.voltaras.organizationservice.dto.request.UpdateOrganizationRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.DuplicateResourceException;
import com.voltaras.organizationservice.exception.ForbiddenOperationException;
import com.voltaras.organizationservice.mapper.OrganizationMapper;
import com.voltaras.organizationservice.mapper.OrganizationMembershipMapper;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.repository.OrganizationRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.impl.OrganizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrganizationServiceImpl} covering creation
 * (owner auto-membership), duplicates, authorization and admin actions.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long ORG_ID = 1L;

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private OrganizationMembershipMapper membershipMapper;
    @Mock private OrganizationAccessHelper accessHelper;

    private OrganizationServiceImpl organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationServiceImpl(
                organizationRepository, membershipRepository,
                organizationMapper, membershipMapper, accessHelper);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Create: success, creator becomes ACTIVE OWNER membership")
    void createOrganization_success_creatorBecomesOwner() {

        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Sunrise Hostel")
                .organizationCode("sunrise-hst")
                .organizationType(OrganizationType.HOSTEL)
                .build();

        Organization organization = Organization.builder()
                .id(ORG_ID)
                .name("Sunrise Hostel")
                .organizationCode("SUNRISE-HST")
                .organizationType(OrganizationType.HOSTEL)
                .status(OrganizationStatus.ACTIVE)
                .build();

        when(organizationRepository.existsByOrganizationCodeIgnoreCase("SUNRISE-HST"))
                .thenReturn(false);
        when(organizationMapper.toEntity(request)).thenReturn(organization);
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);
        when(organizationMapper.toResponse(organization)).thenReturn(OrganizationResponse.builder()
                .id(ORG_ID).organizationCode("SUNRISE-HST").build());

        OrganizationResponse response = organizationService.createOrganization(USER_ID, request);

        assertThat(response.getId()).isEqualTo(ORG_ID);
        assertThat(response.getOrganizationCode()).isEqualTo("SUNRISE-HST");

        // Creator membership must be OWNER + ACTIVE.
        verify(membershipRepository).save(argThat(membership ->
                membership.getOrganization().getId().equals(ORG_ID)
                        && membership.getAuthUserId().equals(USER_ID)
                        && membership.getMembershipRole() == MembershipRole.OWNER
                        && membership.getMembershipStatus() == MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Create: duplicate organization code rejected")
    void createOrganization_duplicateCode_throwsDuplicateResourceException() {

        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Sunrise Hostel")
                .organizationCode("SUNRISE-HST")
                .organizationType(OrganizationType.HOSTEL)
                .build();

        when(organizationRepository.existsByOrganizationCodeIgnoreCase("SUNRISE-HST"))
                .thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(organizationRepository, never()).save(any(Organization.class));
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Get my organizations: returns only ACTIVE memberships")
    void getMyOrganizations_returnsActiveMemberships() {

        OrganizationMembership membership = OrganizationMembership.builder()
                .id(1L)
                .authUserId(USER_ID)
                .membershipRole(MembershipRole.OWNER)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();

        when(membershipRepository
                .findAllByAuthUserIdAndMembershipStatusOrderByCreatedAtDesc(
                        USER_ID, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(membershipMapper.toResponse(membership)).thenReturn(MembershipResponse.builder()
                .id(1L).authUserId(USER_ID).membershipRole(MembershipRole.OWNER).build());

        List<MembershipResponse> response = organizationService.getMyOrganizations(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getMembershipRole()).isEqualTo(MembershipRole.OWNER);
    }

    @Test
    @DisplayName("Get by id: system ADMIN may view any organization")
    void getOrganizationById_systemAdmin_canView() {

        Organization organization = buildOrganization();

        when(accessHelper.isSystemAdmin("ADMIN")).thenReturn(true);
        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(organization);
        when(organizationMapper.toResponse(organization)).thenReturn(OrganizationResponse.builder()
                .id(ORG_ID).build());

        OrganizationResponse response =
                organizationService.getOrganizationById(USER_ID, "ADMIN", ORG_ID);

        assertThat(response.getId()).isEqualTo(ORG_ID);
        verify(accessHelper, never()).getActiveMembership(any(), any());
    }

    @Test
    @DisplayName("Get by id: non-member is forbidden")
    void getOrganizationById_nonMember_throwsForbidden() {

        when(accessHelper.isSystemAdmin("CONSUMER")).thenReturn(false);
        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(buildOrganization());
        when(accessHelper.getActiveMembership(ORG_ID, USER_ID))
                .thenThrow(new ForbiddenOperationException(
                        "You are not an active member of this organization"));

        assertThatThrownBy(() ->
                organizationService.getOrganizationById(USER_ID, "CONSUMER", ORG_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    // ------------------------------------------------------------------
    // Update / deactivate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Update: member without OWNER/ADMIN role is forbidden")
    void updateOrganization_unauthorizedRole_throwsForbidden() {

        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(buildOrganization());
        doThrow(new ForbiddenOperationException("Insufficient organization role"))
                .when(accessHelper).requireOrganizationRole(
                        ORG_ID, USER_ID,
                        MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);

        assertThatThrownBy(() -> organizationService.updateOrganization(
                USER_ID, ORG_ID, new UpdateOrganizationRequest()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Update: inactive organization is rejected")
    void updateOrganization_inactiveOrg_throwsBadRequest() {

        Organization organization = buildOrganization();
        organization.setStatus(OrganizationStatus.INACTIVE);

        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(organization);
        doThrow(new BadRequestException("Organization is not active"))
                .when(accessHelper).requireOrganizationActive(organization);

        assertThatThrownBy(() -> organizationService.updateOrganization(
                USER_ID, ORG_ID, new UpdateOrganizationRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deactivate: owner sets status INACTIVE")
    void deactivateOrganization_owner_success() {

        Organization organization = buildOrganization();

        when(accessHelper.isSystemAdmin("CONSUMER")).thenReturn(false);
        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(organization);
        when(organizationRepository.save(organization)).thenReturn(organization);
        when(organizationMapper.toResponse(organization)).thenReturn(OrganizationResponse.builder()
                .id(ORG_ID).status(OrganizationStatus.INACTIVE).build());

        OrganizationResponse response =
                organizationService.deactivateOrganization(USER_ID, "CONSUMER", ORG_ID);

        assertThat(response.getStatus()).isEqualTo(OrganizationStatus.INACTIVE);
        assertThat(organization.getStatus()).isEqualTo(OrganizationStatus.INACTIVE);
    }

    // ------------------------------------------------------------------
    // System admin
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Admin suspend: non-admin role is rejected")
    void suspendOrganizationForAdmin_nonAdmin_throwsForbidden() {

        doThrow(new ForbiddenOperationException("Only system ADMIN users can perform this operation"))
                .when(accessHelper).requireSystemAdmin("CONSUMER");

        assertThatThrownBy(() ->
                organizationService.suspendOrganizationForAdmin("CONSUMER", ORG_ID))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("Admin suspend: system admin sets status SUSPENDED")
    void suspendOrganizationForAdmin_success() {

        Organization organization = buildOrganization();

        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(organization);
        when(organizationRepository.save(organization)).thenReturn(organization);
        when(organizationMapper.toResponse(organization)).thenReturn(OrganizationResponse.builder()
                .id(ORG_ID).status(OrganizationStatus.SUSPENDED).build());

        OrganizationResponse response =
                organizationService.suspendOrganizationForAdmin("ADMIN", ORG_ID);

        assertThat(response.getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Admin activate: system admin sets status ACTIVE")
    void activateOrganizationForAdmin_success() {

        Organization organization = buildOrganization();
        organization.setStatus(OrganizationStatus.INACTIVE);

        when(accessHelper.requireOrganization(ORG_ID)).thenReturn(organization);
        when(organizationRepository.save(organization)).thenReturn(organization);
        when(organizationMapper.toResponse(organization)).thenReturn(OrganizationResponse.builder()
                .id(ORG_ID).status(OrganizationStatus.ACTIVE).build());

        OrganizationResponse response =
                organizationService.activateOrganizationForAdmin("ADMIN", ORG_ID);

        assertThat(response.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
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
}
