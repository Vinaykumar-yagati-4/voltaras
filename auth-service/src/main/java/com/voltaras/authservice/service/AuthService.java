package com.voltaras.authservice.service;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RefreshTokenRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
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

    UserInfoResponse getCurrentUser(String email);
}