package com.voltaras.apigateway.filter;

import com.voltaras.apigateway.service.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    // Spring automatically injects JwtService through this constructor.
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        String requestPath = exchange
                .getRequest()
                .getURI()
                .getPath();

        // These endpoints do not require a JWT token.
        if (isPublicEndpoint(requestPath)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            return unauthorizedResponse(
                    exchange,
                    "Authorization header is missing or invalid"
            );
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.isValidAccessToken(token)) {

            return unauthorizedResponse(
                    exchange,
                    "Invalid or expired access token"
            );
        }

        try {
            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            ServerHttpRequest modifiedRequest = exchange
                    .getRequest()
                    .mutate()
                    .headers(headers -> {

                        // Prevent clients from sending fake user headers.
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Email");
                        headers.remove("X-User-Role");

                        // Add details extracted from the verified JWT.
                        headers.set(
                                "X-User-Id",
                                String.valueOf(userId)
                        );

                        headers.set(
                                "X-User-Email",
                                email
                        );

                        headers.set(
                                "X-User-Role",
                                role
                        );
                    })
                    .build();

            ServerWebExchange modifiedExchange = exchange
                    .mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);

        } catch (Exception exception) {

            return unauthorizedResponse(
                    exchange,
                    "Unable to read authentication details from token"
            );
        }
    }

    private boolean isPublicEndpoint(String path) {

        return path.equals("/api/auth/signup")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.equals("/actuator")
                || path.startsWith("/actuator/");
    }

    private Mono<Void> unauthorizedResponse(
            ServerWebExchange exchange,
            String message
    ) {

        exchange
                .getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        exchange
                .getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String responseBody = """
                {
                  "status": 401,
                  "error": "UNAUTHORIZED",
                  "message": "%s"
                }
                """.formatted(message);

        byte[] responseBytes =
                responseBody.getBytes(StandardCharsets.UTF_8);

        return exchange
                .getResponse()
                .writeWith(
                        Mono.just(
                                exchange
                                        .getResponse()
                                        .bufferFactory()
                                        .wrap(responseBytes)
                        )
                );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}