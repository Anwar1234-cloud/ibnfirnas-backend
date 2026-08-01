package com.ibnfirnas.config;

import com.ibnfirnas.entity.User;
import com.ibnfirnas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Bootstrap Runner Tests")
class AdminBootstrapRunnerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "");
    }

    @Test
    @DisplayName("No-op when env vars are unset")
    void run_NoEnvVarsSet_DoesNothing() throws Exception {
        runner.run();

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("No-op when only email is set")
    void run_OnlyEmailSet_DoesNothing() throws Exception {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@ibnfirnas.com");

        runner.run();

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("No-op when the account already exists")
    void run_AccountAlreadyExists_DoesNothing() throws Exception {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@ibnfirnas.com");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "supersecret");
        when(userRepository.existsByEmail("admin@ibnfirnas.com")).thenReturn(true);

        runner.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Creates a ROLE_ADMIN account when none exists")
    void run_CreatesAdminAccount_WhenNotExists() throws Exception {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@ibnfirnas.com");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "supersecret");
        when(userRepository.existsByEmail("admin@ibnfirnas.com")).thenReturn(false);
        when(passwordEncoder.encode("supersecret")).thenReturn("hashed");

        runner.run();

        verify(userRepository, times(1)).save(argThat(user ->
                user.getEmail().equals("admin@ibnfirnas.com")
                        && user.getPassword().equals("hashed")
                        && user.getRole().name().equals("ROLE_ADMIN")
                        && user.getIsActive()));
    }
}
