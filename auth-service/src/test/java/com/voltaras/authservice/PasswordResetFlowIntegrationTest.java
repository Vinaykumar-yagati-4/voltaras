package com.voltaras.authservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.authservice.entity.PasswordResetToken;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.repository.PasswordResetTokenRepository;
import com.voltaras.authservice.repository.UserRepository;
import com.voltaras.authservice.service.PasswordResetMailService;
import com.voltaras.authservice.util.PasswordResetTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetFlowIntegrationTest {

    private static final String GENERIC_FORGOT_MESSAGE =
            "If an account exists for that email, password reset instructions have been sent.";

    private static final String RESET_SUCCESS_MESSAGE =
            "Password has been reset successfully. Please log in with your new password.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * The mail sender is mocked so tests can capture the reset link
     * (which carries the raw token) instead of talking to an SMTP
     * server or reading application logs.
     */
    @MockitoBean
    private PasswordResetMailService passwordResetMailService;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void registerUser(String email) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Reset Flow User",
                                  "email": "%s",
                                  "phone": "9876500000",
                                  "address": "123 Test Street",
                                  "password": "CurrentPass123!",
                                  "confirmPassword": "CurrentPass123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private MvcResult performForgot(String email) throws Exception {

        return mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andReturn();
    }

    /**
     * Calls forgot-password and extracts the raw token from the link
     * captured by the mocked mail sender.
     */
    private String requestResetAndCaptureToken(String email) throws Exception {

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(passwordResetMailService, atLeast(1))
                .sendPasswordResetEmail(
                        eq(email),
                        linkCaptor.capture()
                );

        List<String> links = linkCaptor.getAllValues();

        String link = links.get(links.size() - 1);

        return link.substring(
                link.indexOf("?token=") + "?token=".length()
        );
    }

    private MvcResult performReset(
            String token,
            String newPassword,
            String confirmNewPassword
    ) throws Exception {

        return mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "%s",
                                  "confirmNewPassword": "%s"
                                }
                                """.formatted(
                                        token,
                                        newPassword,
                                        confirmNewPassword
                                )))
                .andReturn();
    }

    private int loginStatus(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andReturn();

        return result.getResponse().getStatus();
    }

    private String loginAccessToken(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                        result.getResponse().getContentAsString()
                )
                .get("accessToken")
                .asText();
    }

    // ------------------------------------------------------------------
    // Forgot password
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Forgot password for an existing active email returns the generic response")
    void forgotPasswordExistingActiveEmailReturnsGenericResponse() throws Exception {

        String email = "active-reset@example.com";
        registerUser(email);

        MvcResult result = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("message").asText())
                .isEqualTo(GENERIC_FORGOT_MESSAGE);

        // A token row must have been created for the active user.
        User user = userRepository.findByEmail(email).orElseThrow();
        List<PasswordResetToken> tokens =
                passwordResetTokenRepository.findAllByUser_Id(user.getId());

        assertThat(tokens).hasSize(1);

        // The stored value must be a SHA-256 hash (64 hex chars), never
        // the raw token.
        assertThat(tokens.get(0).getTokenHash())
                .hasSize(64)
                .isNotEqualTo(extractTokenFromLink(lastResetLink(email)));
    }

    private String lastResetLink(String email) {

        ArgumentCaptor<String> linkCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(passwordResetMailService, atLeast(1))
                .sendPasswordResetEmail(
                        eq(email),
                        linkCaptor.capture()
                );

        List<String> links = linkCaptor.getAllValues();
        return links.get(links.size() - 1);
    }

    private String extractTokenFromLink(String link) {
        return link.substring(link.indexOf("?token=") + "?token=".length());
    }

    @Test
    @DisplayName("Forgot password for an unknown email returns the identical generic response")
    void forgotPasswordUnknownEmailReturnsIdenticalGenericResponse() throws Exception {

        String knownEmail = "known-reset@example.com";
        registerUser(knownEmail);

        String knownResponse = performForgot(knownEmail)
                .getResponse()
                .getContentAsString();

        String unknownResponse = performForgot("does-not-exist@example.com")
                .getResponse()
                .getContentAsString();

        assertThat(unknownResponse)
                .isEqualTo(knownResponse);

        JsonNode body = objectMapper.readTree(unknownResponse);
        assertThat(body.get("message").asText())
                .isEqualTo(GENERIC_FORGOT_MESSAGE);

        // No token must exist for the unknown email's "user" (none exists).
        assertThat(userRepository.findByEmail("does-not-exist@example.com"))
                .isEmpty();
    }

    @Test
    @DisplayName("Forgot password for an inactive account returns the generic response and issues no token")
    void forgotPasswordInactiveAccountReturnsGenericResponseAndNoToken() throws Exception {

        String email = "inactive-reset@example.com";

        User inactiveUser = User.builder()
                .fullName("Inactive User")
                .email(email)
                .passwordHash(passwordEncoder.encode("CurrentPass123!"))
                .isActive(false)
                .userRoles(new HashSet<>())
                .build();

        userRepository.save(inactiveUser);

        MvcResult result = performForgot(email);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("message").asText())
                .isEqualTo(GENERIC_FORGOT_MESSAGE);

        assertThat(passwordResetTokenRepository
                .findAllByUser_Id(inactiveUser.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("A second forgot-password request invalidates the previous token")
    void secondForgotPasswordInvalidatesPreviousToken() throws Exception {

        String email = "rotate-token@example.com";
        registerUser(email);

        String firstToken = requestResetAndCaptureToken(email);
        String secondToken = requestResetAndCaptureToken(email);

        // The first token must now be rejected...
        MvcResult firstAttempt = performReset(
                firstToken,
                "NewPass456!",
                "NewPass456!"
        );

        assertThat(firstAttempt.getResponse().getStatus()).isEqualTo(400);

        // ...while the second one works.
        MvcResult secondAttempt = performReset(
                secondToken,
                "NewPass456!",
                "NewPass456!"
        );

        assertThat(secondAttempt.getResponse().getStatus()).isEqualTo(200);
    }

    // ------------------------------------------------------------------
    // Reset password
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A valid token resets the password and the new password works with login")
    void validTokenResetsPasswordAndNewPasswordWorksWithLogin() throws Exception {

        String email = "valid-reset@example.com";
        registerUser(email);

        String token = requestResetAndCaptureToken(email);

        MvcResult resetResult = performReset(
                token,
                "NewPass456!",
                "NewPass456!"
        );

        assertThat(resetResult.getResponse().getStatus()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(
                resetResult.getResponse().getContentAsString()
        );

        assertThat(body.get("message").asText())
                .isEqualTo(RESET_SUCCESS_MESSAGE);

        // The password is encoded - never stored in plain text.
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("NewPass456!", user.getPasswordHash()))
                .isTrue();

        // New password logs in; old password no longer works.
        assertThat(loginStatus(email, "NewPass456!")).isEqualTo(200);
        assertThat(loginStatus(email, "CurrentPass123!")).isEqualTo(401);
    }

    @Test
    @DisplayName("Password mismatch is rejected")
    void passwordMismatchIsRejected() throws Exception {

        String email = "mismatch-reset@example.com";
        registerUser(email);

        String token = requestResetAndCaptureToken(email);

        MvcResult result = performReset(
                token,
                "NewPass456!",
                "DifferentPass456!"
        );

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        // Password must be unchanged.
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(passwordEncoder.matches("CurrentPass123!", user.getPasswordHash()))
                .isTrue();
    }

    @Test
    @DisplayName("Weak/invalid password is rejected")
    void weakPasswordIsRejected() throws Exception {

        String email = "weak-reset@example.com";
        registerUser(email);

        String token = requestResetAndCaptureToken(email);

        MvcResult result = performReset(token, "short", "short");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("VALIDATION_ERROR");

        // Password must be unchanged.
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(passwordEncoder.matches("CurrentPass123!", user.getPasswordHash()))
                .isTrue();
    }

    @Test
    @DisplayName("An invalid token is rejected with a generic error")
    void invalidTokenIsRejected() throws Exception {

        String email = "invalid-token@example.com";
        registerUser(email);

        MvcResult result = performReset(
                "this-token-does-not-exist",
                "NewPass456!",
                "NewPass456!"
        );

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("INVALID_RESET_TOKEN");

        // The error must not leak token or account details.
        String message = body.get("error").get("message").asText();
        assertThat(message).doesNotContain("this-token");
        assertThat(message).doesNotContain(email);
    }

    @Test
    @DisplayName("An expired token is rejected")
    void expiredTokenIsRejected() throws Exception {

        String email = "expired-reset@example.com";
        registerUser(email);

        User user = userRepository.findByEmail(email).orElseThrow();

        String rawToken = PasswordResetTokenUtil.generateRawToken();

        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(PasswordResetTokenUtil.hashToken(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        passwordResetTokenRepository.save(expiredToken);

        MvcResult result = performReset(
                rawToken,
                "NewPass456!",
                "NewPass456!"
        );

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("INVALID_RESET_TOKEN");
    }

    @Test
    @DisplayName("A used token is rejected and cannot be reused")
    void usedTokenIsRejectedAndCannotBeReused() throws Exception {

        String email = "used-reset@example.com";
        registerUser(email);

        String token = requestResetAndCaptureToken(email);

        // First use succeeds.
        assertThat(performReset(token, "NewPass456!", "NewPass456!")
                .getResponse().getStatus())
                .isEqualTo(200);

        // Second use with the same token is rejected.
        MvcResult secondUse = performReset(
                token,
                "AnotherPass789!",
                "AnotherPass789!"
        );

        assertThat(secondUse.getResponse().getStatus()).isEqualTo(400);

        JsonNode body = objectMapper.readTree(
                secondUse.getResponse().getContentAsString()
        );

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("INVALID_RESET_TOKEN");

        // The second password was NOT applied.
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(passwordEncoder.matches("AnotherPass789!", user.getPasswordHash()))
                .isFalse();
    }

    // ------------------------------------------------------------------
    // Security posture
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Reset endpoints work without a JWT")
    void resetEndpointsWorkWithoutJwt() throws Exception {

        // No Authorization header is sent anywhere in this class -
        // these calls must still succeed.
        String email = "public-reset@example.com";
        registerUser(email);

        String token = requestResetAndCaptureToken(email);

        assertThat(performReset(token, "NewPass456!", "NewPass456!")
                .getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Unrelated protected endpoints remain protected")
    void protectedEndpointRemainsProtected() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/auth/profile"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Existing change-password flow keeps working after the reset feature")
    void changePasswordStillWorks() throws Exception {

        String email = "change-password@example.com";
        registerUser(email);

        String accessToken = loginAccessToken(email, "CurrentPass123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "CurrentPass123!",
                                  "newPassword": "NewPass789!",
                                  "confirmNewPassword": "NewPass789!"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(loginStatus(email, "CurrentPass123!")).isEqualTo(401);
        assertThat(loginStatus(email, "NewPass789!")).isEqualTo(200);
    }
}
