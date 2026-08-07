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

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Register then login returns 200 with accessToken instead of 500")
    void registerThenLoginReturnsTokens() throws Exception {

        // ---------- 1. Register ----------
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "John Doe",
                                  "email": "john@example.com",
                                  "phone": "9876543210",
                                  "address": "123 Main Street, Mumbai",
                                  "password": "Secure123!",
                                  "confirmPassword": "Secure123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerBody = objectMapper.readTree(
                registerResult.getResponse().getContentAsString());

        assertThat(registerBody.get("email").asText())
                .isEqualTo("john@example.com");

        // ---------- 2. Login ----------
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "john@example.com",
                                  "password": "Secure123!"
                                }
                                """))
                .andReturn();

        int loginStatus = loginResult.getResponse().getStatus();

        assertThat(loginStatus)
                .as("Login must NOT return HTTP 500 - body was: %s",
                        loginResult.getResponse().getContentAsString())
                .isEqualTo(200);

        JsonNode loginBody = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());

        assertThat(loginBody.get("accessToken").asText())
                .isNotBlank();
        assertThat(loginBody.get("refreshToken").asText())
                .isNotBlank();
        assertThat(loginBody.get("tokenType").asText())
                .isEqualTo("Bearer");
        assertThat(loginBody.get("userId").asLong())
                .isPositive();
    }

    @Test
    @DisplayName("Login with wrong password returns 401 JSON, not 500")
    void loginWithWrongPasswordReturns401() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Jane Doe",
                                  "email": "jane@example.com",
                                  "phone": "9876543211",
                                  "address": "456 Other Street, Delhi",
                                  "password": "Correct123!",
                                  "confirmPassword": "Correct123!"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jane@example.com",
                                  "password": "WrongPassword!"
                                }
                                """))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Wrong password must return 401, body was: %s",
                        result.getResponse().getContentAsString())
                .isEqualTo(401);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Login with unknown email returns 401 JSON, not 500")
    void loginWithUnknownEmailReturns401() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody@example.com",
                                  "password": "Whatever123!"
                                }
                                """))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Unknown email must return 401, body was: %s",
                        result.getResponse().getContentAsString())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("User without role mappings must NOT cause HTTP 500")
    void userWithoutRolesDoesNotCause500() throws Exception {

        /*
         * Simulate a legacy row that has no user_roles mapping.
         * Without null-safety this used to throw inside
         * getAuthorities()/extractRole() and produce HTTP 500.
         */
        User legacyUser = User.builder()
                .fullName("Legacy User")
                .email("legacy@example.com")
                .passwordHash(
                        passwordEncoder.encode("Legacy123!")
                )
                .isActive(true)
                .userRoles(new HashSet<>())
                .build();

        userRepository.save(legacyUser);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "legacy@example.com",
                                  "password": "Legacy123!"
                                }
                                """))
                .andReturn();

        int status = result.getResponse().getStatus();

        assertThat(status)
                .as("User without roles must still log in (200), body was: %s",
                        result.getResponse().getContentAsString())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Protected endpoint without token returns JSON 401, not Whitelabel 500")
    void protectedEndpointWithoutTokenReturnsJson401() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/auth/profile"))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Missing token must return 401, body was: %s",
                        result.getResponse().getContentAsString())
                .isEqualTo(401);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("UNAUTHORIZED");
    }
}
