package com.voltaras.userservice.controller;

import com.voltaras.userservice.dto.request.CreateUserProfileRequest;
import com.voltaras.userservice.dto.request.UpdateUserProfileRequest;
import com.voltaras.userservice.dto.response.ErrorResponse;
import com.voltaras.userservice.dto.response.UserProfileResponse;
import com.voltaras.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(
        name = "User Profile",
        description = "Create, retrieve, update and delete the authenticated user's profile"
)
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(
            summary = "Create user profile",
            description = """
                    Creates a profile for the currently authenticated user.

                    The API Gateway validates the JWT token and injects
                    the authenticated user ID through the X-User-Id header.

                    A user can create only one profile.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User profile created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UserProfileResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid profile information",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "A profile already exists for this user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/profile")
    public ResponseEntity<UserProfileResponse> createProfile(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody CreateUserProfileRequest request
    ) {

        UserProfileResponse response =
                userProfileService.createProfile(
                        authUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Get current user profile",
            description = """
                    Returns the profile belonging to the currently
                    authenticated user.

                    The authenticated user ID is supplied by the
                    API Gateway through the X-User-Id header.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UserProfileResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User profile was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Id") Long authUserId
    ) {

        UserProfileResponse response =
                userProfileService.getProfile(authUserId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update user profile",
            description = """
                    Updates the profile belonging to the currently
                    authenticated user.

                    The authUserId cannot be changed through the request body.
                    It is obtained securely from the API Gateway.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UserProfileResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid profile information",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User profile was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {

        UserProfileResponse response =
                userProfileService.updateProfile(
                        authUserId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete user profile",
            description = """
                    Permanently deletes the profile belonging to the
                    currently authenticated user.

                    This operation deletes only the profile stored in
                    User Service. It does not delete the authentication
                    account stored in Auth Service.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User profile was not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
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