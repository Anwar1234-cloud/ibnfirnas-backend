package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.LoginRequest;
import com.ibnfirnas.dto.request.RegisterRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.AuthResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.entity.enums.UserRole;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;

    @InjectMocks private AuthService authService;

    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .fullName("Anwar Test")
                .email("anwar@test.com")
                .password("hashedPassword")
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();

        registerRequest = new RegisterRequest(
                "Anwar Test", "anwar@test.com", "123456", "+97412345678", "654321");

        loginRequest = new LoginRequest("anwar@test.com", "123456");
    }

    // ============ REGISTER TESTS ============

    @Test
    @DisplayName("Register — success")
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(otpService.verifySmsOtp(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("jwt_token");

        ApiResponse<AuthResponse> response = authService.register(registerRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Registration successful", response.getMessage());
        assertEquals("jwt_token", response.getData().getToken());
        assertEquals("anwar@test.com", response.getData().getEmail());

        verify(userRepository, times(1)).existsByEmail("anwar@test.com");
        verify(otpService, times(1)).verifySmsOtp("+97412345678", "654321");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtTokenProvider, times(1)).generateToken(anyString());
    }

    @Test
    @DisplayName("Register — email already exists")
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email already registered", exception.getMessage());
        verify(otpService, never()).verifySmsOtp(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register — invalid OTP")
    void register_InvalidOtp_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(otpService.verifySmsOtp(anyString(), anyString()))
                .thenThrow(new BadRequestException("Invalid OTP"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(registerRequest));

        assertEquals("Invalid OTP", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register — password is encoded")
    void register_PasswordIsEncoded() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(otpService.verifySmsOtp(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode("123456")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("token");

        authService.register(registerRequest);

        verify(passwordEncoder, times(1)).encode("123456");
        verify(userRepository).save(argThat(user ->
                "hashedPassword".equals(user.getPassword())));
    }

    // ============ LOGIN TESTS ============

    @Test
    @DisplayName("Login — success")
    void login_Success() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateToken("anwar@test.com")).thenReturn("jwt_token");

        ApiResponse<AuthResponse> response = authService.login(loginRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt_token", response.getData().getToken());
        assertEquals("ROLE_USER", response.getData().getRole());
    }

    @Test
    @DisplayName("Login — wrong credentials")
    void login_WrongCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Login — user not found")
    void login_UserNotFound_ThrowsException() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.login(loginRequest));
    }
}