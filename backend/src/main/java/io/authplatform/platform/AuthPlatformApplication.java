package io.authplatform.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for the Authorization Platform.
 *
 * This is a Spring Boot 3 monolithic application that provides:
 * - RBAC-based authorization API
 * - Policy management
 * - User and role management
 * - Audit logging
 *
 * Phase 1 MVP focuses on core RBAC functionality with <10ms p95 latency target.
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class AuthPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthPlatformApplication.class, args);
    }
}
