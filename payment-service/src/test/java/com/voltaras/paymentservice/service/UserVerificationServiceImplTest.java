package com.voltaras.paymentservice.service;

import com.voltaras.paymentservice.client.AuthServiceClient;
import com.voltaras.paymentservice.dto.response.AuthUserResponse;
import com.voltaras.paymentservice.exception.InactiveUserException;
import com.voltaras.paymentservice.exception.UnauthorizedUserException;
import com.voltaras.paymentservice.exception.UpstreamServiceException;
import com.voltaras.paymentservice.exception.UserNotFoundException;
import com.voltaras.paymentservice.service.impl.UserVerificationServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserVerificationServiceImpl}: the Feign call to
 * the Auth Service and the mapping of its responses into the VOLTARAS
 * error codes USER_NOT_FOUND / USER_INACTIVE / UNAUTHORIZED_USER.
 */
@ExtendWith(MockitoExtension.class)
class UserVerificationServiceImplTest {

    private static final Long USER_ID = 100L;

    @Mock private AuthServiceClient authServiceClient;

    private UserVerificationServiceImpl userVerificationService;

    @BeforeEach
    void setUp() {
        userVerificationService =
                new UserVerificationServiceImpl(authServiceClient);
    }

    @Test
    @DisplayName("Active user whose profile matches the headers passes")
    void verify_activeUser_passes() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenReturn(activeUser(USER_ID, "CONSUMER", "user@example.com"));

        assertThatCode(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ROLE_ prefix in the header is normalized before comparing")
    void verify_rolePrefix_normalized() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenReturn(activeUser(USER_ID, "ADMIN", "admin@example.com"));

        assertThatCode(() -> userVerificationService.verifyActiveUser(
                USER_ID, "ROLE_ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Inactive user raises USER_INACTIVE")
    void verify_inactiveUser_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenReturn(AuthUserResponse.builder()
                        .userId(USER_ID)
                        .email("user@example.com")
                        .fullName("User")
                        .role("CONSUMER")
                        .active(false)
                        .build());

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Auth Service 404 raises USER_NOT_FOUND")
    void verify_notFound_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("Auth Service 401/403 (rejected token) raises UNAUTHORIZED_USER")
    void verify_unauthorizedFromAuthService_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenThrow(mock(FeignException.Unauthorized.class));

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(UnauthorizedUserException.class)
                .hasMessageContaining("Auth Service");
    }

    @Test
    @DisplayName("Auth Service failure (5xx/network) raises UPSTREAM_SERVICE_ERROR")
    void verify_upstreamFailure_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenThrow(mock(FeignException.InternalServerError.class));

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("Auth Service is unavailable");
    }

    @Test
    @DisplayName("User ID returned by Auth Service must match X-User-Id")
    void verify_userIdMismatch_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenReturn(activeUser(999L, "CONSUMER", "other@example.com"));

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(UnauthorizedUserException.class)
                .hasMessageContaining("could not be verified");
    }

    @Test
    @DisplayName("Role returned by Auth Service must match X-User-Role")
    void verify_roleMismatch_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenReturn(activeUser(USER_ID, "ADMIN", "user@example.com"));

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(UnauthorizedUserException.class)
                .hasMessageContaining("could not be verified");
    }

    @Test
    @DisplayName("Missing email in the profile raises UNAUTHORIZED_USER")
    void verify_blankEmail_throws() {

        when(authServiceClient.getInternalUser(USER_ID))
                .thenReturn(AuthUserResponse.builder()
                        .userId(USER_ID)
                        .email("  ")
                        .fullName("User")
                        .role("CONSUMER")
                        .active(true)
                        .build());

        assertThatThrownBy(() -> userVerificationService.verifyActiveUser(
                USER_ID, "CONSUMER"))
                .isInstanceOf(UnauthorizedUserException.class)
                .hasMessageContaining("could not be verified");
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private AuthUserResponse activeUser(
            Long userId, String role, String email) {

        return AuthUserResponse.builder()
                .userId(userId)
                .email(email)
                .fullName("Full Name")
                .role(role)
                .active(true)
                .build();
    }
}
