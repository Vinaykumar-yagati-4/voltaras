package com.voltaras.authservice.service;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
import com.voltaras.authservice.dto.response.AuthResponse;
import com.voltaras.authservice.dto.response.UserInfoResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserInfoResponse getCurrentUser(String email);
}