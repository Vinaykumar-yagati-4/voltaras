package com.voltaras.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private static final Logger log =
            LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidAccessToken(String token) {

        try {
            Claims claims = extractAllClaims(token);

            String tokenType =
                    claims.get("tokenType", String.class);

            log.info("JWT subject: {}", claims.getSubject());
            log.info("JWT userId: {}", claims.get("userId"));
            log.info("JWT role: {}", claims.get("role"));
            log.info("JWT tokenType: {}", tokenType);
            log.info("JWT expiration: {}", claims.getExpiration());

            if (tokenType == null) {
                log.error(
                        "JWT validation failed: tokenType claim is missing"
                );

                return false;
            }

            boolean accessToken =
                    "ACCESS".equalsIgnoreCase(tokenType);

            if (!accessToken) {
                log.error(
                        "JWT validation failed: expected ACCESS but found {}",
                        tokenType
                );
            }

            return accessToken;

        } catch (JwtException exception) {

            log.error(
                    "JWT validation failed: {} - {}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            return false;

        } catch (IllegalArgumentException exception) {

            log.error(
                    "JWT token is empty or malformed: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    public Long extractUserId(String token) {

        Object userId = extractAllClaims(token).get("userId");

        if (userId == null) {
            throw new IllegalArgumentException(
                    "userId claim is missing"
            );
        }

        if (userId instanceof Number numberValue) {
            return numberValue.longValue();
        }

        return Long.valueOf(userId.toString());
    }

    public String extractEmail(String token) {

        String email = extractAllClaims(token).getSubject();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT subject/email is missing"
            );
        }

        return email;
    }

    public String extractRole(String token) {

        String role = extractAllClaims(token)
                .get("role", String.class);

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "role claim is missing"
            );
        }

        return role;
    }
}