package com.voltaras.organizationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * Security rules for Swagger, Actuator and application APIs.
     *
     * CORS is intentionally NOT configured in this service.
     *
     * The API Gateway (http://localhost:8080) is the single entry point
     * for all browser traffic and applies the centralized CORS policy
     * (spring.cloud.gateway.server.webflux.globalcors). Requests proxied
     * from the gateway already carry an Origin header, so applying CORS
     * again here either:
     *   - rejects the browser origin (HTTP 403 "Invalid CORS request"),
     *     or
     *   - adds a second Access-Control-Allow-Origin header on top of the
     *     gateway's, which browsers reject ("multiple values").
     * Direct Swagger access (http://localhost:8085 -> same origin) never
     * triggers CORS at all.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Stateless REST API: CSRF protection is not required.
                .csrf(csrf -> csrf.disable())

                // Authentication is handled by the API Gateway.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Permit browser preflight requests.
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Swagger and OpenAPI.
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Actuator.
                        .requestMatchers(
                                "/actuator/**"
                        ).permitAll()

                        // Gateway already validates the JWT.
                        .anyRequest().permitAll()
                )

                // Disable Spring Security login mechanisms.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}
