package com.voltaras.organizationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point for the VOLTARAS Organization Service.
 * <p>
 * The Organization Service manages organizations, memberships, join requests,
 * and the physical structure hierarchy (buildings → blocks → floors → units).
 * Organization membership is optional — normal registration and login through
 * the Auth Service remain available to every user without any organization.
 * <p>
 * The service registers with Eureka and trusts identity from the API Gateway
 * headers ({@code X-User-Id}, {@code X-User-Role}); it never parses JWTs.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class OrganizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationServiceApplication.class, args);
    }
}
