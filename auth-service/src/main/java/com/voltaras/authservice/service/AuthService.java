package com.voltaras.authservice.service;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.ForgotPasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RefreshTokenRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
import com.voltaras.authservice.dto.request.ResetPasswordRequest;
import com.voltaras.authservice.dto.response.AuthResponse;
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
}