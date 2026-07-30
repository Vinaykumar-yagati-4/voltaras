package com.voltaras.authservice.controller;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
import com.voltaras.authservice.dto.response.AuthResponse;
import com.voltaras.authservice.dto.response.UserInfoResponse;
import com.voltaras.authservice.security.CustomUserDetails;
import com.voltaras.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        authService.changePassword(
                userDetails.getUser().getId(),
                request
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password changed successfully"
                )
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        UserInfoResponse response =
                authService.getCurrentUser(
                        userDetails.getUsername()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logged out successfully"
                )
        );
    }
}