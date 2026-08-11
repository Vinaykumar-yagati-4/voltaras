package com.voltaras.metermanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * VOLTARAS Meter Management Service.
 *
 * <p>
 * Source of truth for physical electricity meters: meter details,
 * ownership, assignment and status. Meter Reading Service stores the
 * monthly reading values; this service stores the meter master data.
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MeterManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeterManagementServiceApplication.class, args);
    }
}
