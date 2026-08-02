package com.voltaras.organizationservice.service.impl;

import com.voltaras.organizationservice.dto.request.UpdateMembershipRoleRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.exception.BadRequestException;
import com.voltaras.organizationservice.exception.ResourceNotFoundException;
import com.voltaras.organizationservice.mapper.OrganizationMembershipMapper;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements membership management rules: role changes, suspension and
 * soft removal, with strict OWNER / ORGANIZATION_ADMIN privilege rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipServiceImpl implements MembershipService {

    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationMembershipMapper membershipMapper;
    private final OrganizationAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<MembershipResponse> getOrganizationMembers(
            Long requesterUserId, Long organizationId, Pageable pageable) {

        accessHelper.requireOrganizationRole(
                organizationId, requesterUserId,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN,
                MembershipRole.MANAGER);

        return membershipRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable)
                .map(membershipMapper::toResponse);
    }

    @Override
    @Transactional
    public MembershipResponse updateMembershipRole(
            Long requesterUserId, Long organizationId, Long membershipId,
            UpdateMembershipRoleRequest request) {

        OrganizationMembership requester = accessHelper.requireOrganizationRole(
                organizationId, requesterUserId,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);

        OrganizationMembership target = findMembership(membershipId, organizationId);

        requireActiveMembership(target);

        MembershipRole newRole = request.getRole();

        // Ownership transfer is out of scope: OWNER can never be assigned.
        if (newRole == MembershipRole.OWNER) {
            throw new BadRequestException("OWNER role cannot be assigned");
        }

        // The OWNER membership itself can never be re-roled.
        if (target.getMembershipRole() == MembershipRole.OWNER) {
            throw new BadRequestException("The OWNER role cannot be changed");
        }

        // Only the OWNER may assign or remove the ORGANIZATION_ADMIN role.
        if (newRole == MembershipRole.ORGANIZATION_ADMIN
                || target.getMembershipRole() == MembershipRole.ORGANIZATION_ADMIN) {

            if (requester.getMembershipRole() != MembershipRole.OWNER) {
                throw new BadRequestException(
                        "Only the OWNER can assign or remove the ORGANIZATION_ADMIN role");
            }
        }

        target.setMembershipRole(newRole);

        OrganizationMembership updated = membershipRepository.save(target);

        log.info("Membership role updated: membershipId={}, org={}, newRole={}, by={}",
                membershipId, organizationId, newRole, requesterUserId);

        return membershipMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MembershipResponse suspendMember(
            Long requesterUserId, Long organizationId, Long membershipId) {

        accessHelper.requireOrganizationRole(
                organizationId, requesterUserId,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);

        OrganizationMembership target = findMembership(membershipId, organizationId);

        requireActiveMembership(target);

        if (target.getMembershipRole() == MembershipRole.OWNER) {
            throw new BadRequestException("The OWNER cannot be suspended");
        }

        target.setMembershipStatus(MembershipStatus.SUSPENDED);

        OrganizationMembership updated = membershipRepository.save(target);

        log.info("Membership suspended: membershipId={}, org={}, by={}",
                membershipId, organizationId, requesterUserId);

        return membershipMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse removeMember(
            Long requesterUserId, Long organizationId, Long membershipId) {

        accessHelper.requireOrganizationRole(
                organizationId, requesterUserId,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);

        OrganizationMembership target = findMembership(membershipId, organizationId);

        // Owner self-removal (and any OWNER removal) is blocked while the
        // organization is ACTIVE — the OWNER can never be removed.
        if (target.getMembershipRole() == MembershipRole.OWNER) {
            throw new BadRequestException(
                    "The OWNER cannot remove themselves while the organization is active");
        }

        if (target.getMembershipStatus() == MembershipStatus.REMOVED) {
            throw new BadRequestException("Membership is already removed");
        }

        // Soft removal: the row stays, only the status changes.
        target.setMembershipStatus(MembershipStatus.REMOVED);

        membershipRepository.save(target);

        log.info("Membership removed: membershipId={}, org={}, by={}",
                membershipId, organizationId, requesterUserId);

        return MessageResponse.builder()
                .message("Organization member removed successfully")
                .build();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private OrganizationMembership findMembership(Long membershipId, Long organizationId) {

        return membershipRepository
                .findByIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OrganizationMembership", "id", membershipId));
    }

    private void requireActiveMembership(OrganizationMembership membership) {

        if (membership.getMembershipStatus() != MembershipStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only ACTIVE memberships can be managed");
        }
    }
}
