package com.disputehub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the DisputeHub Portal application.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: Marks class as a source of bean definitions
 * - @EnableAutoConfiguration: Enables Spring Boot's auto-configuration
 * - @ComponentScan: Scans for components, services, controllers in this package
 */
@SpringBootApplication
public class DisputeHubApplication {

    public static void main(String[] args) {
        // Starts the Spring Boot application
        // This will:
        // 1. Start embedded Tomcat server on port 8080
        // 2. Initialize Spring context (dependency injection container)
        // 3. Auto-configure beans based on classpath dependencies
        // 4. Connect to PostgreSQL database
        SpringApplication.run(DisputeHubApplication.class, args);
    }
}
