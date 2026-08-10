package com.voltaras.authservice.service.impl;

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
import com.voltaras.authservice.entity.PasswordResetToken;
import com.voltaras.authservice.entity.RefreshToken;
import com.voltaras.authservice.entity.Role;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.entity.UserRole;
import com.voltaras.authservice.enums.RoleType;
import com.voltaras.authservice.exception.BadRequestException;
import com.voltaras.authservice.exception.DuplicateResourceException;
import com.voltaras.authservice.exception.InvalidResetTokenException;
import com.voltaras.authservice.exception.ResourceNotFoundException;
import com.voltaras.authservice.exception.UnauthorizedException;
import com.voltaras.authservice.repository.PasswordResetTokenRepository;
import com.voltaras.authservice.repository.RefreshTokenRepository;
import com.voltaras.authservice.repository.RoleRepository;
import com.voltaras.authservice.repository.UserRepository;
import com.voltaras.authservice.security.JwtTokenProvider;
import com.voltaras.authservice.service.AuthService;
import com.voltaras.authservice.service.PasswordResetMailService;
import com.voltaras.authservice.util.PasswordResetRateLimiter;
import com.voltaras.authservice.util.PasswordResetTokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordResetRateLimiter passwordResetRateLimiter;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.password-reset.base-url}")
    private String passwordResetBaseUrl;

    @Value("${app.password-reset.expiration-minutes}")
    private int passwordResetExpirationMinutes;

    @Value("${app.refresh-token.cleanup-interval-ms}")
    private long refreshTokenCleanupIntervalMs;

    @Value("${app.refresh-token.retention-days}")
    private int refreshTokenRetentionDays;

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

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setUserRoles(userRoles);

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

        log.debug("Login attempt for email={}", normalizedEmail);

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.getPassword()
                    )
            );

            log.debug("Authentication successful for email={}", normalizedEmail);

        } catch (UsernameNotFoundException exception) {

            log.warn("Login failed - user not found: email={}", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");

        } catch (BadCredentialsException exception) {

            log.warn("Login failed - bad credentials: email={}", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");

        } catch (DisabledException exception) {

            log.warn("Login failed - disabled account: email={}", normalizedEmail);
            throw new DisabledException("Account is deactivated");

        } catch (LockedException exception) {

            log.warn("Login failed - locked account: email={}", normalizedEmail);
            throw new LockedException("Account is locked");

        } catch (InternalAuthenticationServiceException exception) {

            /*
             * Infrastructure failure (e.g. database unreachable while
             * loading the user). Propagate it so the global handler
             * returns a clean JSON 500 (AUTHENTICATION_SERVICE_ERROR)
             * instead of a misleading "invalid credentials" 401.
             */
            log.error(
                    "Login failed - internal authentication error for email={}",
                    normalizedEmail,
                    exception
            );

            throw exception;

        } catch (AuthenticationException exception) {

            /*
             * Any other authentication failure is normalized into a
             * clean 401 instead of leaking as HTTP 500.
             */
            log.warn(
                    "Login failed - unexpected authentication error: email={}, cause={}",
                    normalizedEmail,
                    exception.getMessage()
            );

            throw new BadCredentialsException("Invalid email or password");
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

        log.debug("User found in database: email={}, active={}",
                user.getEmail(), user.getIsActive());

        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        log.debug("Last login timestamp updated for email={}", normalizedEmail);

        /*
         * Every login creates a fresh secure session. The session
         * identifier is embedded in BOTH the access token and the
         * refresh token (sid claim) so logout can revoke the whole
         * session; the refresh token is also persisted server-side
         * (as a SHA-256 hash) so it can be rotated and revoked.
         */
        String sessionId = PasswordResetTokenUtil.generateRawToken();

        String accessToken =
                jwtTokenProvider.generateAccessToken(
                        user,
                        sessionId
                );

        log.debug("Access token generated for email={}", normalizedEmail);

        String refreshToken =
                jwtTokenProvider.generateRefreshToken(
                        user,
                        sessionId
                );

        log.debug("Refresh token generated for email={}", normalizedEmail);

        RefreshToken refreshTokenRecord = RefreshToken.builder()
                .user(user)
                .sessionId(sessionId)
                .tokenHash(
                        PasswordResetTokenUtil.hashToken(
                                refreshToken
                        )
                )
                .issuedAt(LocalDateTime.now())
                .expiresAt(
                        LocalDateTime.now()
                                .plus(
                                        jwtTokenProvider
                                                .getRefreshTokenExpirationMs(),
                                        ChronoUnit.MILLIS
                                )
                )
                .build();

        refreshTokenRepository.save(refreshTokenRecord);

        log.debug(
                "Refresh session persisted for email={}, sessionId={}",
                normalizedEmail,
                sessionId
        );

        String role = extractRole(user);

        log.info(
                "User logged in: email={}, role={}",
                user.getEmail(),
                role
        );

        log.debug("Returning login response for email={}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(accessToken)
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
    @Transactional
    public RefreshTokenResponse refreshToken(
            RefreshTokenRequest request
    ) {

        String rawRefreshToken =
                request.getRefreshToken().trim();

        /*
         * Reject access tokens, expired tokens, tampered tokens and
         * tokens with invalid signatures before touching the database.
         */
        if (!jwtTokenProvider.validateRefreshToken(
                rawRefreshToken
        )) {

            throw new UnauthorizedException(
                    "Invalid or expired refresh token"
            );
        }

        /*
         * Only a persisted refresh session may be used. Tokens created
         * before this feature (never persisted) and unknown tokens are
         * rejected here - there is no silent fallback.
         */
        String tokenHash =
                PasswordResetTokenUtil.hashToken(
                        rawRefreshToken
                );

        /*
         * The pessimistic write lock serializes concurrent refresh
         * attempts with the same token: the losing transaction sees
         * the record as already revoked/replaced and is rejected, so
         * the same token can never succeed twice.
         */
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid or expired refresh token"
                        )
                );

        /*
         * Reject records that are expired, already revoked (logout) or
         * already replaced (previous rotation).
         */
        if (stored.getRevokedAt() != null
                || stored.getReplacedByTokenHash() != null
                || stored.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            throw new UnauthorizedException(
                    "Invalid or expired refresh token"
            );
        }

        /*
         * Verify that the JWT claims match the persisted record so a
         * record can never be replayed under a different identity or
         * session.
         */
        String tokenEmail =
                jwtTokenProvider.getEmailFromToken(
                        rawRefreshToken
                );

        String tokenSessionId =
                jwtTokenProvider.getSessionIdFromToken(
                        rawRefreshToken
                );

        Long tokenUserId =
                jwtTokenProvider.getUserIdFromToken(
                        rawRefreshToken
                );

        User user = stored.getUser();

        if (tokenEmail == null
                || tokenSessionId == null
                || tokenUserId == null
                || !user.getEmail().equalsIgnoreCase(tokenEmail)
                || !stored.getSessionId().equals(tokenSessionId)
                || !user.getId().equals(tokenUserId)) {

            throw new UnauthorizedException(
                    "Invalid or expired refresh token"
            );
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {

            throw new UnauthorizedException(
                    "User account is inactive"
            );
        }

        /*
         * Rotate the refresh token: the session survives, but the old
         * token is revoked and replaced by a new one in the same
         * transaction.
         */
        String newAccessToken =
                jwtTokenProvider.generateAccessToken(
                        user,
                        stored.getSessionId()
                );

        String newRefreshToken =
                jwtTokenProvider.generateRefreshToken(
                        user,
                        stored.getSessionId()
                );

        LocalDateTime now = LocalDateTime.now();

        stored.setRevokedAt(now);
        stored.setReplacedByTokenHash(
                PasswordResetTokenUtil.hashToken(
                        newRefreshToken
                )
        );

        refreshTokenRepository.save(stored);

        RefreshToken replacement = RefreshToken.builder()
                .user(user)
                .sessionId(stored.getSessionId())
                .tokenHash(
                        PasswordResetTokenUtil.hashToken(
                                newRefreshToken
                        )
                )
                .issuedAt(now)
                .expiresAt(
                        now.plus(
                                jwtTokenProvider
                                        .getRefreshTokenExpirationMs(),
                                ChronoUnit.MILLIS
                        )
                )
                .build();

        refreshTokenRepository.save(replacement);

        String role = extractRole(user);

        log.info(
                "Tokens refreshed for userId={}, sessionId={}",
                user.getId(),
                stored.getSessionId()
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
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        String normalizedEmail =
                request.getEmail()
                        .toLowerCase()
                        .trim();

        /*
         * Protect the public endpoint from repeated abuse. The check
         * runs before the user lookup so unknown emails are treated
         * exactly like known ones.
         */
        passwordResetRateLimiter.check(normalizedEmail);

        /*
         * Deliberately silent for unknown or inactive accounts:
         * the caller always receives the same generic response, so
         * account existence/status is never revealed.
         */
        userRepository.findByEmail(normalizedEmail)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .ifPresent(user -> {

                    // Invalidate any previous unused tokens.
                    passwordResetTokenRepository
                            .deleteByUser_IdAndUsedAtIsNull(user.getId());

                    String rawToken =
                            PasswordResetTokenUtil.generateRawToken();

                    PasswordResetToken token =
                            PasswordResetToken.builder()
                                    .user(user)
                                    .tokenHash(
                                            PasswordResetTokenUtil.hashToken(rawToken)
                                    )
                                    .expiresAt(
                                            LocalDateTime.now()
                                                    .plusMinutes(
                                                            passwordResetExpirationMinutes
                                                    )
                                    )
                                    .build();

                    passwordResetTokenRepository.save(token);

                    String resetLink =
                            passwordResetBaseUrl
                                    + "?token="
                                    + rawToken;

                    try {

                        passwordResetMailService
                                .sendPasswordResetEmail(
                                        normalizedEmail,
                                        resetLink
                                );

                    } catch (Exception exception) {

                        /*
                         * A mail delivery failure must not turn into
                         * an error response: doing so would let an
                         * attacker distinguish existing accounts.
                         * The token simply expires unused.
                         */
                        log.error(
                                "Failed to send password reset email for userId={}",
                                user.getId(),
                                exception
                        );
                    }

                    log.info(
                            "Password reset token issued for userId={}",
                            user.getId()
                    );
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmNewPassword())) {

            throw new BadRequestException(
                    "Passwords do not match"
            );
        }

        String tokenHash =
                PasswordResetTokenUtil.hashToken(
                        request.getToken().trim()
                );

        /*
         * Pessimistic write lock serializes concurrent attempts with
         * the same token: the second transaction sees the token as
         * already used and is rejected.
         */
        PasswordResetToken token =
                passwordResetTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new InvalidResetTokenException(
                                        "Invalid or expired password reset token"
                                )
                        );

        if (token.getUsedAt() != null
                || token.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            throw new InvalidResetTokenException(
                    "Invalid or expired password reset token"
            );
        }

        User user = token.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        // Single-use: mark this token consumed in the same transaction.
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        // Invalidate every other active token of the same user.
        passwordResetTokenRepository.deleteByUser_IdAndIdNot(
                user.getId(),
                token.getId()
        );

        /*
         * A password reset is performed by someone who may not control
         * the existing sessions, so every active refresh session of
         * the user is revoked in the same transaction. The client must
         * log in again with the new password.
         */
        int revokedSessions =
                refreshTokenRepository.revokeAllActiveByUserId(
                        user.getId(),
                        LocalDateTime.now()
                );

        log.info(
                "Password reset completed for userId={}, revokedRefreshSessions={}",
                user.getId(),
                revokedSessions
        );
    }

    @Override
    @Transactional
    public void logout(Long userId, String sessionId) {

        /*
         * The session identifier comes from the verified access token
         * in the security context, never from request headers.
         *
         * Access tokens issued before the session feature carry no
         * sid claim; for them there is no persisted session to revoke
         * (legacy refresh tokens are rejected by /refresh-token
         * because they were never persisted), so logout is a safe no-op.
         */
        if (sessionId == null || sessionId.isBlank()) {

            log.warn(
                    "Logout requested without a session identifier for userId={}",
                    userId
            );

            return;
        }

        int revoked = refreshTokenRepository
                .revokeActiveBySessionId(
                        userId,
                        sessionId,
                        LocalDateTime.now()
                );

        log.info(
                "Logout completed for userId={}, sessionId={}, revokedRecords={}",
                userId,
                sessionId,
                revoked
        );
    }

    /**
     * Periodically purges expired (unused) refresh-token records and
     * records revoked longer ago than the retention window. Active
     * sessions are never deleted.
     */
    @Scheduled(fixedDelayString = "${app.refresh-token.cleanup-interval-ms}")
    @Transactional
    public void cleanupExpiredRefreshTokens() {

        LocalDateTime now = LocalDateTime.now();

        int deleted = refreshTokenRepository
                .deleteExpiredAndOldRevoked(
                        now,
                        now.minusDays(
                                refreshTokenRetentionDays
                        )
                );

        if (deleted > 0) {

            log.info(
                    "Cleaned up {} expired/revoked refresh token records",
                    deleted
            );
        }
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

    @Override
    @Transactional(readOnly = true)
    public InternalUserResponse getUserById(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "id",
                                userId
                        )
                );

        /*
         * Null-safe internal lookup (consuming services must never see a
         * 500 for legacy or partially-migrated rows): a missing role
         * falls back to CONSUMER and a missing active flag defaults to
         * true.
         */
        Boolean isActive = user.getIsActive();

        return InternalUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(extractRole(user))
                .active(isActive == null || isActive)
                .build();
    }

    /**
     * Extracts the first assigned role from the user.
     * Falls back to CONSUMER when the user has no roles.
     */
    private String extractRole(User user) {

        if (user.getUserRoles() == null
                || user.getUserRoles().isEmpty()) {

            return RoleType.CONSUMER.name();
        }

        return user.getUserRoles()
                .stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(role ->
                        role
                                .getName()
                                .name()
                )
                .findFirst()
                .orElse(RoleType.CONSUMER.name());
    }
}