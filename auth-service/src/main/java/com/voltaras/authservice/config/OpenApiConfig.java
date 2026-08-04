package com.voltaras.authservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI authServiceOpenAPI() {

        Server gatewayServer = new Server()
                .url("http://localhost:8080")
                .description("VOLTARAS API Gateway");

        Server authServiceServer = new Server()
                .url("http://localhost:8081")
                .description("Auth Service - Direct Local Access");

        Contact contact = new Contact()
                .name("VOLTARAS Development Team");

        Info apiInfo = new Info()
                .title("VOLTARAS Auth Service API")
                .version("1.0.0")
                .description("""
                        REST API documentation for the VOLTARAS Auth Service.

                        This service manages:

                        • User registration
                        • User login
                        • JWT token generation
                        • Refresh tokens
                        • Password management
                        • Current user details
                        """)
                .contact(contact);

        SecurityScheme bearerSecurityScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Paste the JWT access token received from the login API.

                        Paste only the token value.
                        Do not type the word Bearer.
                        Swagger adds Bearer automatically.
                        """);

        SecurityRequirement securityRequirement =
                new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME);

        Components components = new Components()
                .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        bearerSecurityScheme
                );

        return new OpenAPI()
                .info(apiInfo)
                .servers(List.of(
                        gatewayServer,
                        authServiceServer
                ))
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}