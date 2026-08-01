package com.ibnfirnas.config;

import com.ibnfirnas.entity.User;
import com.ibnfirnas.entity.enums.UserRole;
import com.ibnfirnas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Provisions the first admin account from env vars on startup, since there's
 * no other way to get a ROLE_ADMIN user without direct DB access. Opt-in
 * (no-op if the env vars aren't set) and idempotent (no-op if an account
 * with that email already exists) — safe to leave the env vars set across
 * every future restart/deploy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Value("${admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            return;
        }

        if (userRepository.existsByEmail(bootstrapEmail)) {
            log.info("Admin bootstrap: account for {} already exists, skipping", bootstrapEmail);
            return;
        }

        User admin = User.builder()
                .fullName("Admin")
                .email(bootstrapEmail)
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(UserRole.ROLE_ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("Admin bootstrap: created admin account for {}", bootstrapEmail);
    }
}
