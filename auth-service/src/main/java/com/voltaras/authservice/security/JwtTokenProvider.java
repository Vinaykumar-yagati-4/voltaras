package com.voltaras.authservice.security;

import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.enums.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log =
            LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    /**
     * Creates the JWT signing key and reads both token expiration periods.
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-expiration}") long refreshTokenExpirationMs
    ) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Generates a short-lived access token bound to the given session.
     * The sessionId is embedded as the "sid" claim so logout can
     * revoke the matching refresh session.
     */
    public String generateAccessToken(
            User user,
            String sessionId
    ) {

        return generateToken(
                user,
                ACCESS_TOKEN_TYPE,
                accessTokenExpirationMs,
                sessionId
        );
    }

    /**
     * Generates a long-lived refresh token bound to the same session
     * as its matching access token.
     */
    public String generateRefreshToken(
            User user,
            String sessionId
    ) {

        return generateToken(
                user,
                REFRESH_TOKEN_TYPE,
                refreshTokenExpirationMs,
                sessionId
        );
    }

    /**
     * Common method used to generate access and refresh tokens.
     *
     * Each token carries a random jti and the session's secure
     * identifier (sid) shared by the access/refresh pair.
     */
    private String generateToken(
            User user,
            String tokenType,
            long expiration,
            String sessionId
    ) {

        Date issuedAt = new Date();

        Date expiresAt =
                new Date(issuedAt.getTime() + expiration);

        String role = extractRoleFromUser(user);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", role)
                .claim("tokenType", tokenType)
                .claim("sid", sessionId)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the user's role from the UserRole relationship.
     * Falls back to CONSUMER when the user has no roles.
     */
    private String extractRoleFromUser(User user) {

        if (user.getUserRoles() == null
                || user.getUserRoles().isEmpty()) {

            return RoleType.CONSUMER.name();
        }

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

    /**
     * Extracts email from the JWT subject.
     */
    public String getEmailFromToken(String token) {

        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the user ID from the token, or null when the claim is
     * missing.
     */
    public Long getUserIdFromToken(String token) {

        Object userId = parseClaims(token).get("userId");

        if (userId == null) {
            return null;
        }

        if (userId instanceof Number numberValue) {
            return numberValue.longValue();
        }

        return Long.valueOf(userId.toString());
    }

    /**
     * Extracts the secure session identifier (sid claim) from the
     * token, or null when the claim is missing.
     */
    public String getSessionIdFromToken(String token) {

        return parseClaims(token)
                .get("sid", String.class);
    }

    /**
     * Extracts the role from the token.
     */
    public String getRoleFromToken(String token) {

        return parseClaims(token)
                .get("role", String.class);
    }

    /**
     * Extracts ACCESS or REFRESH token type.
     */
    public String getTokenTypeFromToken(String token) {

        return parseClaims(token)
                .get("tokenType", String.class);
    }

    /**
     * Validates signature, structure and expiration.
     */
    public boolean validateToken(String token) {

        try {
            parseClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException exception) {

            log.warn(
                    "Invalid JWT token: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    /**
     * Validates that the supplied token is an ACCESS token.
     */
    public boolean validateAccessToken(String token) {

        return validateTokenType(
                token,
                ACCESS_TOKEN_TYPE
        );
    }

    /**
     * Validates that the supplied token is a REFRESH token.
     */
    public boolean validateRefreshToken(String token) {

        return validateTokenType(
                token,
                REFRESH_TOKEN_TYPE
        );
    }

    /**
     * Validates token signature, expiration and tokenType claim.
     */
    private boolean validateTokenType(
            String token,
            String requiredTokenType
    ) {

        try {
            Claims claims = parseClaims(token);

            String actualTokenType =
                    claims.get("tokenType", String.class);

            return requiredTokenType.equalsIgnoreCase(
                    actualTokenType
            );

        } catch (JwtException | IllegalArgumentException exception) {

            log.warn(
                    "Invalid {} token: {}",
                    requiredTokenType,
                    exception.getMessage()
            );

            return false;
        }
    }

    /**
     * Parses claims and verifies the JWT signature.
     */
    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {

        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {

        return refreshTokenExpirationMs;
    }

    /**
     * Kept temporarily for compatibility with existing code.
     */
    public long getExpirationMs() {

        return accessTokenExpirationMs;
    }
}