package com.voltaras.organizationservice.service;

import com.voltaras.organizationservice.dto.request.UpdateMembershipRoleRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.mapper.OrganizationMembershipMapper;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MembershipServiceImpl} covering role updates,
 * suspension, soft removal and the OWNER privilege rules.
 */
@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long ORG_ADMIN_ID = 2L;
    private static final Long MEMBER_ID = 100L;
    private static final Long ORG_ID = 1L;
    private static final Long TARGET_MEMBERSHIP_ID = 10L;

    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private OrganizationMembershipMapper membershipMapper;
    @Mock private OrganizationAccessHelper accessHelper;

    private MembershipServiceImpl membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipServiceImpl(
                membershipRepository, membershipMapper, accessHelper);
    }

    // ------------------------------------------------------------------
    // List
    // ------------------------------------------------------------------

    @Test
    @DisplayName("List members: MANAGER may view members")
    void getOrganizationMembers_manager_success() {

        OrganizationMembership membership = buildMembership(MEMBER_ID, MembershipRole.MEMBER);

        when(membershipRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                ORG_ID, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(membership)));
        when(membershipMapper.toResponse(membership)).thenReturn(MembershipResponse.builder()
                .id(TARGET_MEMBERSHIP_ID).membershipRole(MembershipRole.MEMBER).build());

        Page<MembershipResponse> response =
                membershipService.getOrganizationMembers(
                       MEMBER_ID, ORG_ID, PageRequest.of(0, 10));
        assertThat(response.getContent()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // Update role
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Update role: OWNER assigns ORGANIZATION_ADMIN")
    void updateMembershipRole_ownerAssignsAdmin_success() {

        OrganizationMembership target = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.MEMBER);

         when(accessHelper.requireOrganizationRole(
        ORG_ID, OWNER_ID,
        MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN))
        .thenReturn(buildMembership(OWNER_ID, MembershipRole.OWNER));


        when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(target));
        when(membershipRepository.save(target)).thenReturn(target);
        when(membershipMapper.toResponse(target)).thenReturn(MembershipResponse.builder()
                .id(TARGET_MEMBERSHIP_ID).membershipRole(MembershipRole.ORGANIZATION_ADMIN).build());

        MembershipResponse response = membershipService.updateMembershipRole(
                OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID,
                UpdateMembershipRoleRequest.builder().role(MembershipRole.ORGANIZATION_ADMIN).build());

        assertThat(response.getMembershipRole()).isEqualTo(MembershipRole.ORGANIZATION_ADMIN);
    }

    @Test
    @DisplayName("Update role: ORGANIZATION_ADMIN cannot assign ORGANIZATION_ADMIN")
    void updateMembershipRole_orgAdminAssignsAdmin_throwsBadRequest() {

        OrganizationMembership target = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.MEMBER);

               when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(target));

                 when(accessHelper.requireOrganizationRole(
        ORG_ID, ORG_ADMIN_ID,
        MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN))
        .thenReturn(buildMembership(ORG_ADMIN_ID, MembershipRole.ORGANIZATION_ADMIN));

        assertThatThrownBy(() -> membershipService.updateMembershipRole(
                ORG_ADMIN_ID, ORG_ID, TARGET_MEMBERSHIP_ID,
                UpdateMembershipRoleRequest.builder()
                        .role(MembershipRole.ORGANIZATION_ADMIN).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only the OWNER");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update role: assigning OWNER is blocked (no ownership transfer)")
    void updateMembershipRole_assignOwner_throwsBadRequest() {

        OrganizationMembership target = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.MEMBER);

when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() -> membershipService.updateMembershipRole(
                OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID,
                UpdateMembershipRoleRequest.builder().role(MembershipRole.OWNER).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be assigned");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update role: the OWNER membership role cannot be changed")
    void updateMembershipRole_changeOwnerRole_throwsBadRequest() {

        OrganizationMembership ownerMembership = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.OWNER);


        when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> membershipService.updateMembershipRole(
                OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID,
                UpdateMembershipRoleRequest.builder().role(MembershipRole.MANAGER).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be changed");
    }

    // ------------------------------------------------------------------
    // Suspend
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Suspend: ACTIVE member suspended by OWNER")
    void suspendMember_success() {

        OrganizationMembership target = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.MEMBER);


        when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(target));
        when(membershipRepository.save(target)).thenReturn(target);
        when(membershipMapper.toResponse(target)).thenReturn(MembershipResponse.builder()
                .id(TARGET_MEMBERSHIP_ID).membershipStatus(MembershipStatus.SUSPENDED).build());

        MembershipResponse response =
                membershipService.suspendMember(OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID);

        assertThat(response.getMembershipStatus()).isEqualTo(MembershipStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Suspend: the OWNER can never be suspended")
    void suspendMember_ownerTarget_throwsBadRequest() {

        OrganizationMembership ownerMembership = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.OWNER);

               when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() ->
                membershipService.suspendMember(OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be suspended");

        verify(membershipRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Remove
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Remove: member soft-removed to REMOVED with confirmation message")
    void removeMember_success_softRemoval() {

        OrganizationMembership target = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.MEMBER);


        when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(target));
        when(membershipRepository.save(target)).thenReturn(target);

        MessageResponse response =
                membershipService.removeMember(OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID);

        assertThat(response.getMessage()).isEqualTo("Organization member removed successfully");
        assertThat(target.getMembershipStatus()).isEqualTo(MembershipStatus.REMOVED);
    }

    @Test
    @DisplayName("Remove: OWNER cannot remove themselves while active")
    void removeMember_ownerSelf_throwsBadRequest() {

        OrganizationMembership ownerMembership = buildMembership(
                TARGET_MEMBERSHIP_ID, MembershipRole.OWNER);
        ownerMembership.setAuthUserId(OWNER_ID);

               when(membershipRepository.findByIdAndOrganizationId(TARGET_MEMBERSHIP_ID, ORG_ID))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() ->
                membershipService.removeMember(OWNER_ID, ORG_ID, TARGET_MEMBERSHIP_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot remove themselves");

        verify(membershipRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private OrganizationMembership buildMembership(Long id, MembershipRole role) {
        return OrganizationMembership.builder()
                .id(id)
                .authUserId(MEMBER_ID)
                .membershipRole(role)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
    }
}
