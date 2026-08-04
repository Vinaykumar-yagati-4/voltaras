package com.voltaras.authservice.service.impl;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RefreshTokenRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
import com.voltaras.authservice.dto.response.AuthResponse;
import com.voltaras.authservice.dto.response.RefreshTokenResponse;
import com.voltaras.authservice.dto.response.UserInfoResponse;
import com.voltaras.authservice.entity.Role;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.entity.UserRole;
import com.voltaras.authservice.enums.RoleType;
import com.voltaras.authservice.exception.BadRequestException;
import com.voltaras.authservice.exception.DuplicateResourceException;
import com.voltaras.authservice.exception.ResourceNotFoundException;
import com.voltaras.authservice.exception.UnauthorizedException;
import com.voltaras.authservice.repository.RoleRepository;
import com.voltaras.authservice.repository.UserRepository;
import com.voltaras.authservice.security.JwtTokenProvider;
import com.voltaras.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {

            throw new BadRequestException(
                    "Passwords do not match"
            );
        }

        if (request.getFullName() == null ||
                request.getFullName().trim().isEmpty()) {

            throw new BadRequestException(
                    "Full name is required"
            );
        }

        String normalizedEmail =
                request.getEmail()
                        .toLowerCase()
                        .trim();

        if (userRepository.existsByEmail(normalizedEmail)) {

            throw new DuplicateResourceException(
                    "User",
                    "email",
                    normalizedEmail
            );
        }

        String hashedPassword =
                passwordEncoder.encode(
                        request.getPassword()
                );

        Role consumerRole = roleRepository
                .findByName(RoleType.CONSUMER)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Default role CONSUMER was not found"
                        )
                );

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .isActive(true)
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(consumerRole)
                .build();

        user.setUserRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);

        log.info(
                "New user registered: email={}, role={}",
                savedUser.getEmail(),
                RoleType.CONSUMER
        );

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(RoleType.CONSUMER.name())
                .message(
                        "Registration successful. Please log in."
                )
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        String normalizedEmail =
                request.getEmail()
                        .toLowerCase()
                        .trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.getPassword()
                    )
            );

        } catch (BadCredentialsException exception) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );

        } catch (DisabledException exception) {

            throw new DisabledException(
                    "Account is deactivated"
            );
        }

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                normalizedEmail
                        )
                );

        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        String accessToken =
                jwtTokenProvider.generateAccessToken(user);

        String refreshToken =
                jwtTokenProvider.generateRefreshToken(user);

        String role = extractRole(user);

        log.info(
                "User logged in: email={}, role={}",
                user.getEmail(),
                role
        );

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(
                        jwtTokenProvider
                                .getAccessTokenExpirationMs()
                                / 1000
                )
                .refreshTokenExpiresIn(
                        jwtTokenProvider
                                .getRefreshTokenExpirationMs()
                                / 1000
                )
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(role)
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(
            RefreshTokenRequest request
    ) {

        String refreshToken =
                request.getRefreshToken().trim();

        /*
         * Reject access tokens, expired tokens and tokens with
         * invalid signatures.
         */
        if (!jwtTokenProvider.validateRefreshToken(
                refreshToken
        )) {

            throw new UnauthorizedException(
                    "Invalid or expired refresh token"
            );
        }

        String email =
                jwtTokenProvider.getEmailFromToken(
                        refreshToken
                );

        User user = userRepository
                .findByEmail(email.toLowerCase().trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                email
                        )
                );

        if (Boolean.FALSE.equals(user.getIsActive())) {

            throw new UnauthorizedException(
                    "User account is inactive"
            );
        }

        /*
         * Refresh-token rotation:
         * create both a new access token and a new refresh token.
         */
        String newAccessToken =
                jwtTokenProvider.generateAccessToken(user);

        String newRefreshToken =
                jwtTokenProvider.generateRefreshToken(user);

        String role = extractRole(user);

        log.info(
                "Tokens refreshed for userId={}, email={}",
                user.getId(),
                user.getEmail()
        );

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(
                        jwtTokenProvider
                                .getAccessTokenExpirationMs()
                                / 1000
                )
                .refreshTokenExpiresIn(
                        jwtTokenProvider
                                .getRefreshTokenExpirationMs()
                                / 1000
                )
                .userId(user.getId())
                .role(role)
                .email(user.getEmail())
                .message("Tokens refreshed successfully")
                .build();
    }

    @Override
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {

        if (!request.getNewPassword()
                .equals(request.getConfirmNewPassword())) {

            throw new BadRequestException(
                    "New passwords do not match"
            );
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "id",
                                userId
                        )
                );

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash()
        )) {

            throw new BadRequestException(
                    "Current password is incorrect"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        log.info(
                "Password changed for userId={}",
                userId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String email) {

        String normalizedEmail =
                email.toLowerCase().trim();

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                normalizedEmail
                        )
                );

        String role = extractRole(user);

        return UserInfoResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(role)
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Extracts the first assigned role from the user.
     */
    private String extractRole(User user) {

        return user.getUserRoles()
                .stream()
                .findFirst()
                .map(userRole ->
                        userRole
                                .getRole()
                                .getName()
                                .name()
                )
                .orElse(RoleType.CONSUMER.name());
    }
}