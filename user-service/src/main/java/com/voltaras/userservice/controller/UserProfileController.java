package com.voltaras.userservice.controller;

import com.voltaras.userservice.dto.request.CreateUserProfileRequest;
import com.voltaras.userservice.dto.request.UpdateUserProfileRequest;
import com.voltaras.userservice.dto.response.UserProfileResponse;
import com.voltaras.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/profile")
    public ResponseEntity<UserProfileResponse> createProfile(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody CreateUserProfileRequest request
    ) {

        UserProfileResponse response =
                userProfileService.createProfile(authUserId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Id") Long authUserId
    ) {

        UserProfileResponse response =
                userProfileService.getProfile(authUserId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {

        UserProfileResponse response =
                userProfileService.updateProfile(authUserId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Map<String, String>> deleteProfile(
            @RequestHeader("X-User-Id") Long authUserId
    ) {

        userProfileService.deleteProfile(authUserId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User profile deleted successfully"
                )
        );
    }
}