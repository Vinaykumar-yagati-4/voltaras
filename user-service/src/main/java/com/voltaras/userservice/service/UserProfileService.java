package com.voltaras.userservice.service;

import com.voltaras.userservice.dto.request.CreateUserProfileRequest;
import com.voltaras.userservice.dto.request.UpdateUserProfileRequest;
import com.voltaras.userservice.dto.response.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse createProfile(
            Long authUserId,
            CreateUserProfileRequest request
    );

    UserProfileResponse getProfile(Long authUserId);

    UserProfileResponse updateProfile(
            Long authUserId,
            UpdateUserProfileRequest request
    );

    void deleteProfile(Long authUserId);
}