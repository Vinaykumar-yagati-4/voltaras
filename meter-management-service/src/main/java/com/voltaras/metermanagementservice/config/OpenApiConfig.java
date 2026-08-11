package com.voltaras.metermanagementservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meterManagementOpenAPI() {

        Server gatewayServer = new Server()
                .url("http://localhost:8080")
                .description("API Gateway URL");

        Server directServiceServer = new Server()
                .url("http://localhost:8089")
                .description("Meter Management Service Direct URL");

        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste only the JWT token. Swagger will add Bearer automatically.");

        return new OpenAPI()
                .servers(List.of(gatewayServer, directServiceServer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth))
                .info(new Info()
                        .title("VOLTARAS Meter Management Service APIs")
                        .description("APIs for managing electricity meters.")
                        .version("1.0.0"));
    }
}