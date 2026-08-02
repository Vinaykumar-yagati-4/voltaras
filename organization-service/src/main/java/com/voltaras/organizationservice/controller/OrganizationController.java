package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.dto.request.CreateOrganizationRequest;
import com.voltaras.organizationservice.dto.request.UpdateOrganizationRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody CreateOrganizationRequest request) {

        OrganizationResponse response =
                organizationService.createOrganization(authUserId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<MembershipResponse>> getMyOrganizations(
            @RequestHeader("X-User-Id") Long authUserId) {

        List<MembershipResponse> response =
                organizationService.getMyOrganizations(authUserId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId) {

        OrganizationResponse response =
                organizationService.getOrganizationById(
                        authUserId, systemRole, organizationId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {

        OrganizationResponse response =
                organizationService.updateOrganization(
                        authUserId, organizationId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/deactivate")
    public ResponseEntity<OrganizationResponse> deactivateOrganization(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId) {

        OrganizationResponse response =
                organizationService.deactivateOrganization(
                        authUserId, systemRole, organizationId);

        return ResponseEntity.ok(response);
    }
}
