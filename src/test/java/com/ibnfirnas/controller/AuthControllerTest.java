package com.ibnfirnas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibnfirnas.dto.request.LoginRequest;
import com.ibnfirnas.dto.request.RegisterRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.AuthResponse;
import com.ibnfirnas.service.AuthService;
import com.ibnfirnas.service.GoogleAuthService;
import com.ibnfirnas.security.JwtTokenProvider;
import com.ibnfirnas.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ibnfirnas.security.CustomUserDetailsService;
import com.ibnfirnas.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean private GoogleAuthService googleAuthService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register — success")
    void register_Returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Anwar Test");
        request.setEmail("anwar@test.com");
        request.setPassword("123456");
        request.setPhone("+974123");

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt_token")
                .email("anwar@test.com")
                .fullName("Anwar Test")
                .role("ROLE_USER")
                .build();

        when(authService.register(any())).thenReturn(
                ApiResponse.success("Registration successful", authResponse));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt_token"))
                .andExpect(jsonPath("$.data.email").value("anwar@test.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register — missing fields returns 400")
    void register_MissingFields_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(); // all fields blank

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login — success")
    void login_Returns200() throws Exception {
        LoginRequest request = new LoginRequest("anwar@test.com", "123456");

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt_token")
                .email("anwar@test.com")
                .fullName("Anwar Test")
                .role("ROLE_USER")
                .build();

        when(authService.login(any())).thenReturn(
                ApiResponse.success("Login successful", authResponse));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/auth/me — unauthorized without token")
    void getMe_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "anwar@test.com", roles = "USER")
    @DisplayName("POST /api/auth/refresh-token — success with auth")
    void refreshToken_WithAuth_Returns200() throws Exception {
        when(jwtTokenProvider.generateToken(any())).thenReturn("new_token");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}