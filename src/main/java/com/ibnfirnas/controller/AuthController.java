package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.GoogleAuthRequest;
import com.ibnfirnas.dto.request.LoginRequest;
import com.ibnfirnas.dto.request.RegisterRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.AuthResponse;
import com.ibnfirnas.dto.response.RefreshTokenResponse;
import com.ibnfirnas.dto.response.UserResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.security.JwtTokenProvider;
import com.ibnfirnas.service.AuthService;
import com.ibnfirnas.service.GoogleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }


    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleAuthRequest request) {

        AuthResponse response =
                googleAuthService.authenticate(request.getIdToken());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Google login successful",
                        response
                )
        );
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BadCredentialsException("Unauthorized");
        }

        String newToken = jwtTokenProvider.generateToken(userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Token refreshed",
                        RefreshTokenResponse.builder()
                                .accessToken(newToken)
                                .tokenType("Bearer")
                                .build()
                )
        );
    }


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BadCredentialsException("Unauthorized");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Current user", response)
        );
    }
}