package com.voltaras.userservice.service.impl;

import com.voltaras.userservice.dto.request.CreateUserProfileRequest;
import com.voltaras.userservice.dto.request.UpdateUserProfileRequest;
import com.voltaras.userservice.dto.response.UserProfileResponse;
import com.voltaras.userservice.entity.UserProfile;
import com.voltaras.userservice.exception.DuplicateResourceException;
import com.voltaras.userservice.exception.ResourceNotFoundException;
import com.voltaras.userservice.repository.UserProfileRepository;
import com.voltaras.userservice.service.UserProfileService;
import com.voltaras.userservice.util.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log =
            LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public UserProfileResponse createProfile(
            Long authUserId,
            CreateUserProfileRequest request
    ) {

        if (userProfileRepository.existsByAuthUserId(authUserId)) {
            throw new DuplicateResourceException(
                    "UserProfile",
                    "authUserId",
                    authUserId
            );
        }

        UserProfile profile =
                UserProfileMapper.toEntity(request);

        profile.setAuthUserId(authUserId);

        UserProfile savedProfile =
                userProfileRepository.save(profile);

        log.info(
                "User profile created: authUserId={}, profileId={}",
                savedProfile.getAuthUserId(),
                savedProfile.getId()
        );

        return UserProfileMapper.toResponse(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long authUserId) {

        UserProfile profile =
                findProfileByAuthUserId(authUserId);

        return UserProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(
            Long authUserId,
            UpdateUserProfileRequest request
    ) {

        UserProfile profile =
                findProfileByAuthUserId(authUserId);

        UserProfileMapper.copyToEntity(request, profile);

        UserProfile updatedProfile =
                userProfileRepository.save(profile);

        log.info(
                "User profile updated: authUserId={}, profileId={}",
                updatedProfile.getAuthUserId(),
                updatedProfile.getId()
        );

        return UserProfileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long authUserId) {

        UserProfile profile =
                findProfileByAuthUserId(authUserId);

        userProfileRepository.delete(profile);

        log.info(
                "User profile deleted: authUserId={}, profileId={}",
                authUserId,
                profile.getId()
        );
    }

    private UserProfile findProfileByAuthUserId(Long authUserId) {

        return userProfileRepository
                .findByAuthUserId(authUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "UserProfile",
                                "authUserId",
                                authUserId
                        )
                );
    }
}