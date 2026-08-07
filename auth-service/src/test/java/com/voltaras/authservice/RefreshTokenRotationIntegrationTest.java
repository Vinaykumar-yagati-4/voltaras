package com.voltaras.authservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.authservice.entity.RefreshToken;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.repository.RefreshTokenRepository;
import com.voltaras.authservice.repository.UserRepository;
import com.voltaras.authservice.security.JwtTokenProvider;
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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Used only by the password-reset revocation test to capture the
     * raw reset token from the (mocked) email link.
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
                                  "fullName": "Refresh Flow User",
                                  "email": "%s",
                                  "phone": "9876510000",
                                  "address": "123 Refresh Street",
                                  "password": "CurrentPass123!",
                                  "confirmPassword": "CurrentPass123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "CurrentPass123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
    }

    private MvcResult refresh(String refreshToken) throws Exception {

        return mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andReturn();
    }

    private MvcResult logout(String accessToken) throws Exception {

        return mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    private String resetTokenFromMail(String email) throws Exception {

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

    private int resetStatus(String token, String newPassword) throws Exception {

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
                                        newPassword
                                )))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    // ------------------------------------------------------------------
    // Test A - normal login and refresh
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Test A: login then refresh returns 200 with a new access and refresh token")
    void loginThenRefreshReturnsNewTokens() throws Exception {

        String email = "test-a-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String accessTokenA1 = loginBody.get("accessToken").asText();
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        MvcResult refreshResult = refresh(refreshTokenR1);

        assertThat(refreshResult.getResponse().getStatus()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(
                refreshResult.getResponse().getContentAsString()
        );

        String accessTokenA2 = body.get("accessToken").asText();
        String refreshTokenR2 = body.get("refreshToken").asText();

        assertThat(accessTokenA2).isNotBlank().isNotEqualTo(accessTokenA1);
        assertThat(refreshTokenR2).isNotBlank().isNotEqualTo(refreshTokenR1);
        assertThat(body.get("message").asText())
                .isEqualTo("Tokens refreshed successfully");
    }

    // ------------------------------------------------------------------
    // Test B - rotation: reused token rejected, replacement works
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Test B: a rotated-out refresh token is rejected, its replacement works")
    void rotatedOutTokenIsRejectedAndReplacementWorks() throws Exception {

        String email = "test-b-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        // First refresh succeeds and rotates R1 -> R2.
        MvcResult firstRefresh = refresh(refreshTokenR1);
        assertThat(firstRefresh.getResponse().getStatus()).isEqualTo(200);

        JsonNode firstBody = objectMapper.readTree(
                firstRefresh.getResponse().getContentAsString()
        );

        String refreshTokenR2 = firstBody.get("refreshToken").asText();

        // Reusing R1 must now fail with 401.
        MvcResult reused = refresh(refreshTokenR1);
        assertThat(reused.getResponse().getStatus()).isEqualTo(401);

        JsonNode reuseBody = objectMapper.readTree(
                reused.getResponse().getContentAsString()
        );

        assertThat(reuseBody.get("error").get("code").asText())
                .isEqualTo("UNAUTHORIZED");

        // The replacement R2 still works.
        MvcResult secondRefresh = refresh(refreshTokenR2);
        assertThat(secondRefresh.getResponse().getStatus()).isEqualTo(200);
    }

    // ------------------------------------------------------------------
    // Test C - logout revokes the matching refresh token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Test C: logout revokes the matching refresh token and is idempotent")
    void logoutRevokesRefreshToken() throws Exception {

        String email = "test-c-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String accessTokenA1 = loginBody.get("accessToken").asText();
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        // Logout must succeed and be idempotent.
        assertThat(logout(accessTokenA1).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(logout(accessTokenA1).getResponse().getStatus())
                .isEqualTo(200);

        // The refresh token of the logged-out session must now be rejected.
        MvcResult result = refresh(refreshTokenR1);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(body.get("error").get("code").asText())
                .isEqualTo("UNAUTHORIZED");
    }

    // ------------------------------------------------------------------
    // Test D - logout after rotation revokes the whole session
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Test D: logout after rotation revokes the current and previous refresh tokens")
    void logoutAfterRotationRevokesSession() throws Exception {

        String email = "test-d-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String accessTokenA1 = loginBody.get("accessToken").asText();
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        // Rotate R1 -> R2 (same session).
        MvcResult refreshResult = refresh(refreshTokenR1);
        assertThat(refreshResult.getResponse().getStatus()).isEqualTo(200);

        JsonNode refreshBody = objectMapper.readTree(
                refreshResult.getResponse().getContentAsString()
        );

        String accessTokenA2 = refreshBody.get("accessToken").asText();
        String refreshTokenR2 = refreshBody.get("refreshToken").asText();

        // Logout with the NEW access token A2.
        assertThat(logout(accessTokenA2).getResponse().getStatus())
                .isEqualTo(200);

        // R2 (current) and R1 (rotated out) must both be rejected.
        assertThat(refresh(refreshTokenR2).getResponse().getStatus())
                .isEqualTo(401);
        assertThat(refresh(refreshTokenR1).getResponse().getStatus())
                .isEqualTo(401);
    }

    // ------------------------------------------------------------------
    // Additional security tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("An expired refresh token is rejected")
    void expiredRefreshTokenIsRejected() throws Exception {

        String email = "expired-refresh@example.com";
        registerUser(email);

        User user = userRepository.findByEmail(email).orElseThrow();

        // A properly signed refresh token whose persisted record is
        // already past its expiry.
        String sessionId = PasswordResetTokenUtil.generateRawToken();
        String rawToken = jwtTokenProvider.generateRefreshToken(user, sessionId);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .sessionId(sessionId)
                .tokenHash(PasswordResetTokenUtil.hashToken(rawToken))
                .issuedAt(LocalDateTime.now().minusDays(8))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build());

        MvcResult result = refresh(rawToken);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("A tampered refresh token is rejected")
    void tamperedRefreshTokenIsRejected() throws Exception {

        String email = "tampered-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        char first = refreshTokenR1.charAt(0);
        String tampered = (first == 'a' ? "b" : "a")
                + refreshTokenR1.substring(1);

        assertThat(refresh(tampered).getResponse().getStatus())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("An access token cannot be submitted as a refresh token")
    void accessTokenCannotBeUsedAsRefreshToken() throws Exception {

        String email = "access-as-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String accessTokenA1 = loginBody.get("accessToken").asText();

        MvcResult result = refresh(accessTokenA1);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("A refresh token cannot be used as a Bearer access token on protected endpoints")
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {

        String email = "refresh-as-bearer@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        MvcResult result = mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + refreshTokenR1))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("An unknown or legacy (never persisted) refresh token is rejected")
    void unknownRefreshTokenIsRejected() throws Exception {

        String email = "unknown-refresh@example.com";
        registerUser(email);

        User user = userRepository.findByEmail(email).orElseThrow();

        // Validly signed refresh token with NO database record - this is
        // what every token issued before this feature looks like.
        String orphanToken = jwtTokenProvider.generateRefreshToken(
                user,
                PasswordResetTokenUtil.generateRawToken()
        );

        MvcResult result = refresh(orphanToken);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Logout only revokes its own session, other sessions keep working")
    void logoutOnlyAffectsItsOwnSession() throws Exception {

        String email = "two-sessions-refresh@example.com";
        registerUser(email);

        // Two independent logins = two independent sessions.
        JsonNode session1 = login(email);
        JsonNode session2 = login(email);

        String accessTokenS1 = session1.get("accessToken").asText();
        String refreshTokenS1 = session1.get("refreshToken").asText();
        String refreshTokenS2 = session2.get("refreshToken").asText();

        assertThat(logout(accessTokenS1).getResponse().getStatus())
                .isEqualTo(200);

        // Session 1 is revoked, session 2 is untouched.
        assertThat(refresh(refreshTokenS1).getResponse().getStatus())
                .isEqualTo(401);
        assertThat(refresh(refreshTokenS2).getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Two simultaneous refresh requests with the same token: only one succeeds")
    void concurrentRefreshOnlyOneSucceeds() throws Exception {

        String email = "concurrent-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Queue<Integer> statuses = new ConcurrentLinkedQueue<>();

        Runnable task = () -> {
            try {
                start.await();
                int status = refresh(refreshTokenR1)
                        .getResponse()
                        .getStatus();
                statuses.add(status);
            } catch (Exception exception) {
                statuses.add(-1);
            }
        };

        Future<?> first = executor.submit(task);
        Future<?> second = executor.submit(task);

        start.countDown();
        first.get(30, TimeUnit.SECONDS);
        second.get(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(statuses.stream().sorted().toList())
                .as("Exactly one concurrent refresh must win (200) and one must lose (401)")
                .containsExactly(200, 401);
    }

    @Test
    @DisplayName("Only SHA-256 hashes of refresh tokens are stored, never the raw token")
    void onlyTokenHashesAreStored() throws Exception {

        String email = "hash-storage-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        User user = userRepository.findByEmail(email).orElseThrow();

        List<RefreshToken> records =
                refreshTokenRepository.findAllByUser_Id(user.getId());

        assertThat(records).hasSize(1);

        RefreshToken record = records.get(0);

        assertThat(record.getTokenHash())
                .hasSize(64)
                .isNotEqualTo(refreshTokenR1);
        assertThat(record.getTokenHash())
                .isEqualTo(PasswordResetTokenUtil.hashToken(refreshTokenR1));
        assertThat(record.getSessionId()).isNotBlank();
    }

    @Test
    @DisplayName("Password reset revokes all active refresh sessions of the user")
    void passwordResetRevokesAllActiveRefreshSessions() throws Exception {

        String email = "reset-revokes-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        String resetToken = resetTokenFromMail(email);

        assertThat(resetStatus(resetToken, "NewPass456!"))
                .isEqualTo(200);

        // The pre-reset refresh session is revoked.
        MvcResult result = refresh(refreshTokenR1);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        // The new password works and creates a fresh, usable session.
        MvcResult newLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "NewPass456!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode newLogin = objectMapper.readTree(
                newLoginResult.getResponse().getContentAsString()
        );

        assertThat(refresh(
                newLogin.get("refreshToken").asText()
        ).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Change password keeps working and does not silently break refresh sessions")
    void changePasswordStillWorks() throws Exception {

        String email = "change-password-refresh@example.com";
        registerUser(email);

        JsonNode loginBody = login(email);
        String accessTokenA1 = loginBody.get("accessToken").asText();
        String refreshTokenR1 = loginBody.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessTokenA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "CurrentPass123!",
                                  "newPassword": "ChangedPass789!",
                                  "confirmNewPassword": "ChangedPass789!"
                                }
                                """))
                .andExpect(status().isOk());

        // Documented behavior: change-password does not terminate the
        // existing session (only password reset and logout do).
        assertThat(refresh(refreshTokenR1).getResponse().getStatus())
                .isEqualTo(200);
    }
}
