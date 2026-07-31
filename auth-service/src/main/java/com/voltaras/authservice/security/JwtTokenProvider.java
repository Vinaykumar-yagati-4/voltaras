package com.voltaras.authservice.security;

import com.voltaras.authservice.entity.User;
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

@Component
public class JwtTokenProvider {

    private static final Logger log =
            LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Creates the JWT signing key using the Base64 secret
     * configured in application.yml.
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationMs
    ) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates an access token for the authenticated user.
     */
    public String generateToken(User user) {

        Date now = new Date();

        Date expiryDate =
                new Date(now.getTime() + expirationMs);

        String role = user.getUserRoles()
                .stream()
                .findFirst()
                .map(userRole ->
                        userRole
                                .getRole()
                                .getName()
                                .name()
                )
                .orElse("CONSUMER");

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", role)
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts email from JWT subject.
     */
    public String getEmailFromToken(String token) {

        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the authenticated user's ID.
     */
    public Long getUserIdFromToken(String token) {

        Object userId = parseClaims(token).get("userId");

        if (userId instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        if (userId instanceof Long longValue) {
            return longValue;
        }

        if (userId instanceof Number numberValue) {
            return numberValue.longValue();
        }

        return Long.valueOf(userId.toString());
    }

    /**
     * Extracts user role.
     */
    public String getRoleFromToken(String token) {

        return parseClaims(token)
                .get("role", String.class);
    }

    /**
     * Extracts token type.
     */
    public String getTokenTypeFromToken(String token) {

        return parseClaims(token)
                .get("tokenType", String.class);
    }

    /**
     * Validates JWT signature and expiration.
     */
    public boolean validateToken(String token) {

        try {
            parseClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException exception) {

            log.error(
                    "Invalid JWT token: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    /**
     * Validates whether the JWT is an ACCESS token.
     */
    public boolean validateAccessToken(String token) {

        try {
            Claims claims = parseClaims(token);

            String tokenType =
                    claims.get("tokenType", String.class);

            return "ACCESS".equalsIgnoreCase(tokenType);

        } catch (JwtException | IllegalArgumentException exception) {

            log.error(
                    "Invalid access token: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    /**
     * Parses all JWT claims and validates the signature.
     */
    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {

        return expirationMs;
    }
}