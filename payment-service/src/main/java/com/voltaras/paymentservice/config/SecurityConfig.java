package com.voltaras.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security rules for the Payment Service.
 *
 * <p>
 * Authentication is handled by the API Gateway (JWT validation). This
 * service only:
 * </p>
 *
 * <ul>
 *     <li>disables CSRF (stateless REST API)</li>
 *     <li>uses stateless sessions</li>
 *     <li>permits browser preflight OPTIONS requests</li>
 *     <li>permits Swagger/OpenAPI and Actuator endpoints</li>
 *     <li>permits application APIs (authorization happens in the service
 *         layer via the X-User-Id / X-User-Role headers)</li>
 * </ul>
 *
 * <p>
 * CORS is intentionally NOT configured here: it is centralized at the
 * API Gateway (spring.cloud.gateway.server.webflux.globalcors).
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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

                        // Gateway already validates the JWT; role checks
                        // are enforced in the service layer. The Razorpay
                        // webhook is additionally protected by the
                        // RAZORPAY_WEBHOOK_SECRET shared secret.
                        .anyRequest().permitAll()
                )

                // Disable Spring Security login mechanisms.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}
