package com.voltaras.complaintservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VOLTARAS Complaint Service.
 *
 * <p>
 * Authentication is handled by the API Gateway (JWT validation). The
 * authenticated user identity and role are received through the
 * {@code X-User-Id} and {@code X-User-Role} headers injected by the
 * Gateway; authorization is enforced in the service layer.
 * </p>
 */
@SpringBootApplication
public class ComplaintServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplaintServiceApplication.class, args);
    }
}
