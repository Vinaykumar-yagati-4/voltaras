package com.voltaras.organizationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Disable CSRF because this service is stateless.
                .csrf(csrf -> csrf.disable())

                // Enable CORS with Spring defaults.
                .cors(Customizer.withDefaults())

                // Stateless because authentication is handled by API Gateway.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ---------- Swagger/OpenAPI ----------
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ---------- Actuator ----------
                        .requestMatchers(
                                "/actuator/**"
                        ).permitAll()

                        // ---------- Application APIs ----------
                        .anyRequest().permitAll()
                )

                // Disable default Spring Security login pages.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}