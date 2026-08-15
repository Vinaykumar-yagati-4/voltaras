package com.voltaras.organizationservice.service.impl;

import com.voltaras.organizationservice.dto.request.CreateOrganizationRequest;
import com.voltaras.organizationservice.dto.request.UpdateOrganizationRequest;
import com.voltaras.organizationservice.dto.response.AvailableOrganizationResponse;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import com.voltaras.organizationservice.exception.DuplicateResourceException;
import com.voltaras.organizationservice.mapper.OrganizationMapper;
import com.voltaras.organizationservice.mapper.OrganizationMembershipMapper;
import com.voltaras.organizationservice.repository.OrganizationMembershipRepository;
import com.voltaras.organizationservice.repository.OrganizationRepository;
import com.voltaras.organizationservice.security.OrganizationAccessHelper;
import com.voltaras.organizationservice.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Implements the organization business rules. The creator of an
 * organization automatically becomes its ACTIVE OWNER in the same
 * transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMembershipMapper membershipMapper;
    private final OrganizationAccessHelper accessHelper;

    @Override
    @Transactional
    public OrganizationResponse createOrganization(
            Long authUserId, CreateOrganizationRequest request) {

        accessHelper.requireAuthenticatedUser(authUserId);

        // Organization codes are unique case-insensitively and normalized.
        String normalizedCode = normalizeCode(request.getOrganizationCode());

        if (organizationRepository.existsByOrganizationCodeIgnoreCase(normalizedCode)) {
            log.warn("Duplicate organization code rejected: {}", normalizedCode);
            throw new DuplicateResourceException(
                    "Organization", "organizationCode", normalizedCode);
        }

        Organization organization = organizationMapper.toEntity(request);
        organization.setOrganizationCode(normalizedCode);
        organization.setCreatedByAuthUserId(authUserId);
        organization.setStatus(OrganizationStatus.ACTIVE);

        Organization saved = organizationRepository.save(organization);

        // Creator automatically becomes an ACTIVE OWNER.
        OrganizationMembership membership = OrganizationMembership.builder()
                .organization(saved)
                .authUserId(authUserId)
                .membershipRole(MembershipRole.OWNER)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();

        membershipRepository.save(membership);

        log.info("Organization created: organizationId={}, code={}, createdBy={}",
                saved.getId(), saved.getOrganizationCode(), authUserId);

        return organizationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> getMyOrganizations(Long authUserId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        return membershipRepository
                .findAllByAuthUserIdAndMembershipStatusOrderByCreatedAtDesc(
                        authUserId, MembershipStatus.ACTIVE)
                .stream()
                .map(membershipMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableOrganizationResponse> getAvailableOrganizations(Long authUserId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        return organizationRepository
                .findAllByStatusOrderByCreatedAtDesc(OrganizationStatus.ACTIVE)
                .stream()
                .map(organizationMapper::toAvailableResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(
            Long authUserId, String systemRole, Long organizationId) {

        Organization organization = accessHelper.requireOrganization(organizationId);

        if (!accessHelper.isSystemAdmin(systemRole)) {
            // ACTIVE member (any role) may view the organization.
            accessHelper.getActiveMembership(organizationId, authUserId);
        }

        return organizationMapper.toResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse updateOrganization(
            Long authUserId, Long organizationId, UpdateOrganizationRequest request) {

        Organization organization = accessHelper.requireOrganization(organizationId);

        accessHelper.requireOrganizationRole(
                organizationId, authUserId,
                MembershipRole.OWNER, MembershipRole.ORGANIZATION_ADMIN);

        accessHelper.requireOrganizationActive(organization);

        organizationMapper.updateEntity(request, organization);

        Organization updated = organizationRepository.save(organization);

        log.info("Organization updated: organizationId={}, updatedBy={}",
                organizationId, authUserId);

        return organizationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public OrganizationResponse deactivateOrganization(
            Long authUserId, String systemRole, Long organizationId) {

        Organization organization = accessHelper.requireOrganization(organizationId);

        if (!accessHelper.isSystemAdmin(systemRole)) {
            accessHelper.requireOrganizationRole(
                    organizationId, authUserId, MembershipRole.OWNER);
        }

        organization.setStatus(OrganizationStatus.INACTIVE);

        Organization updated = organizationRepository.save(organization);

        log.info("Organization deactivated: organizationId={}, by={}",
                organizationId, authUserId);

        return organizationMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getAllOrganizationsForAdmin(
            String systemRole, OrganizationStatus status,
            OrganizationType type, Pageable pageable) {

        accessHelper.requireSystemAdmin(systemRole);

        Page<Organization> page;

        if (status != null && type != null) {
            page = organizationRepository
                    .findAllByStatusAndOrganizationTypeOrderByCreatedAtDesc(
                            status, type, pageable);
        } else if (status != null) {
            page = organizationRepository
                    .findAllByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (type != null) {
            page = organizationRepository
                    .findAllByOrganizationTypeOrderByCreatedAtDesc(type, pageable);
        } else {
            page = organizationRepository
                    .findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(organizationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationForAdmin(
            String systemRole, Long organizationId) {

        accessHelper.requireSystemAdmin(systemRole);

        Organization organization = accessHelper.requireOrganization(organizationId);

        return organizationMapper.toResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse suspendOrganizationForAdmin(
            String systemRole, Long organizationId) {

        accessHelper.requireSystemAdmin(systemRole);

        Organization organization = accessHelper.requireOrganization(organizationId);

        organization.setStatus(OrganizationStatus.SUSPENDED);

        Organization updated = organizationRepository.save(organization);

        log.info("Organization suspended by system admin: organizationId={}",
                organizationId);

        return organizationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public OrganizationResponse activateOrganizationForAdmin(
            String systemRole, Long organizationId) {

        accessHelper.requireSystemAdmin(systemRole);

        Organization organization = accessHelper.requireOrganization(organizationId);

        organization.setStatus(OrganizationStatus.ACTIVE);

        Organization updated = organizationRepository.save(organization);

        log.info("Organization activated by system admin: organizationId={}",
                organizationId);

        return organizationMapper.toResponse(updated);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
