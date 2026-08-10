package com.voltaras.authservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the internal service-to-service user lookup used by the
 * Payment Service ({@code GET /api/auth/internal/users/{userId}}).
 *
 * <p>
 * The endpoint must be reachable WITHOUT a Bearer token (services call it
 * load-balanced by service name), must return the exact fields the
 * consuming services expect (userId, email, fullName, role, active) and
 * must be null-safe: missing roles fall back to CONSUMER and a missing
 * active flag defaults to true. Unknown users and unknown paths must
 * return JSON 404 - never 500.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class InternalUserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Internal user lookup without token returns the expected profile fields")
    void internalUserLookupReturnsProfileWithoutToken() throws Exception {

        // ---------- Register (role CONSUMER is assigned automatically) ----------
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Sunny Kumar",
                                  "email": "sunny@gmail.com",
                                  "phone": "9876543999",
                                  "address": "1 Voltaras Street, Bengaluru",
                                  "password": "Secure123!",
                                  "confirmPassword": "Secure123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerBody = objectMapper.readTree(
                registerResult.getResponse().getContentAsString());

        long userId = registerBody.get("userId").asLong();

        // ---------- Internal lookup WITHOUT any Authorization header ----------
        MvcResult lookupResult = mockMvc.perform(
                        get("/api/auth/internal/users/" + userId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(
                lookupResult.getResponse().getContentAsString());

        assertThat(body.get("userId").asLong())
                .isEqualTo(userId);
        assertThat(body.get("email").asText())
                .isEqualTo("sunny@gmail.com");
        assertThat(body.get("fullName").asText())
                .isEqualTo("Sunny Kumar");
        assertThat(body.get("role").asText())
                .isEqualTo("CONSUMER");
        assertThat(body.get("active").asBoolean())
                .isTrue();
    }

    @Test
    @DisplayName("Internal lookup of an unknown user returns JSON 404, not 500")
    void internalUserLookupUnknownUserReturns404() throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/auth/internal/users/999999"))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Unknown user must return 404, body was: %s",
                        result.getResponse().getContentAsString())
                .isEqualTo(404);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Unknown internal path returns JSON 404, not 500")
    void unknownInternalPathReturns404() throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/auth/internal/nonexistent"))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Unknown path must return 404, body was: %s",
                        result.getResponse().getContentAsString())
                .isEqualTo(404);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("User without role mappings falls back to CONSUMER instead of 500")
    void userWithoutRolesIsNullSafe() throws Exception {

        /*
         * Simulate a legacy row that has no user_roles mapping: the
         * internal lookup must default to CONSUMER instead of throwing
         * and producing HTTP 500.
         *
         * (A NULL is_active cannot be represented in this schema - the
         * column is NOT NULL - so the active field is verified as true
         * with the normal value.)
         */
        User legacyUser = User.builder()
                .fullName("Legacy User")
                .email("legacy-internal@example.com")
                .passwordHash(
                        passwordEncoder.encode("Legacy123!")
                )
                .isActive(true)
                .userRoles(new HashSet<>())
                .build();

        User saved = userRepository.save(legacyUser);

        MvcResult result = mockMvc.perform(
                        get("/api/auth/internal/users/" + saved.getId()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());

        assertThat(body.get("role").asText())
                .isEqualTo("CONSUMER");
        assertThat(body.get("active").asBoolean())
                .isTrue();
    }
}
