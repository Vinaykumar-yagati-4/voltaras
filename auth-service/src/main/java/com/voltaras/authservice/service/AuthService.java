package com.voltaras.authservice.service;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.ForgotPasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RefreshTokenRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
import com.voltaras.authservice.dto.request.ResetPasswordRequest;
import com.voltaras.authservice.dto.response.AuthResponse;
import com.voltaras.authservice.dto.response.InternalUserResponse;
import com.voltaras.authservice.dto.response.RefreshTokenResponse;
import com.voltaras.authservice.dto.response.UserInfoResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(
            RefreshTokenRequest request
    );

    void changePassword(
            Long userId,
            ChangePasswordRequest request
    );

    /**
     * Starts the password-reset flow for the given email. Always
     * produces the same generic outcome whether or not the account
     * exists and whether or not it is active.
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Completes the password reset using a single-use token.
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Revokes every active refresh token of the login session that
     * issued the authenticated access token. Idempotent.
     */
    void logout(Long userId, String sessionId);

    UserInfoResponse getCurrentUser(String email);

    /**
     * Internal service-to-service lookup of the user profile with the
     * given ID.
     *
     * <p>
     * Used by other VOLTARAS services (e.g. the Payment Service) to
     * verify that a user exists, is active and to obtain its role before
     * wallet operations. The lookup is null-safe: a missing role falls
     * back to CONSUMER and a missing active flag defaults to true.
     * </p>
     *
     * @param userId the user ID to look up
     * @return the internal user profile
     * @throws com.voltaras.authservice.exception.ResourceNotFoundException
     *         when no user exists with the given ID
     */
    InternalUserResponse getUserById(Long userId);
}