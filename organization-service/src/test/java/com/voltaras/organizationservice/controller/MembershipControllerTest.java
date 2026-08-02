package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.config.SecurityConfig;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import com.voltaras.organizationservice.service.MembershipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link MembershipController}: member list, role
 * change, suspend and soft-remove.
 */
@WebMvcTest(MembershipController.class)
@Import(SecurityConfig.class)
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipService membershipService;

    @Test
    @DisplayName("GET members: 200 OK with paginated list")
    void getOrganizationMembers_returns200Ok() throws Exception {

        MembershipResponse response = MembershipResponse.builder()
                .id(10L)
                .organizationId(1L)
                .authUserId(100L)
                .membershipRole(MembershipRole.MEMBER)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();

        when(membershipService.getOrganizationMembers(eq(1L), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/organizations/1/members")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].membershipRole").value("MEMBER"));
    }

    @Test
    @DisplayName("PATCH role: 200 OK")
    void updateMembershipRole_returns200Ok() throws Exception {

        MembershipResponse response = MembershipResponse.builder()
                .id(10L)
                .membershipRole(MembershipRole.ORGANIZATION_ADMIN)
                .build();

        when(membershipService.updateMembershipRole(eq(1L), eq(1L), eq(10L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/organizations/1/members/10/role")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ORGANIZATION_ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipRole").value("ORGANIZATION_ADMIN"));
    }

    @Test
    @DisplayName("PATCH role: missing role fails validation with 400")
    void updateMembershipRole_missingRole_returns400() throws Exception {

        mockMvc.perform(patch("/api/organizations/1/members/10/role")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH suspend: 200 OK")
    void suspendMember_returns200Ok() throws Exception {

        MembershipResponse response = MembershipResponse.builder()
                .id(10L)
                .membershipStatus(MembershipStatus.SUSPENDED)
                .build();

        when(membershipService.suspendMember(1L, 1L, 10L)).thenReturn(response);

        mockMvc.perform(patch("/api/organizations/1/members/10/suspend")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipStatus").value("SUSPENDED"));
    }

    @Test
    @DisplayName("DELETE member: 200 OK with confirmation message")
    void removeMember_returns200WithMessage() throws Exception {

        when(membershipService.removeMember(1L, 1L, 10L))
                .thenReturn(MessageResponse.builder()
                        .message("Organization member removed successfully")
                        .build());

        mockMvc.perform(delete("/api/organizations/1/members/10")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Organization member removed successfully"));
    }
}
