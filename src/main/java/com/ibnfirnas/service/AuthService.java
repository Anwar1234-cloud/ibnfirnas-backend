package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.LoginRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.AuthResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;


    public ApiResponse<AuthResponse> login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = jwtTokenProvider.generateToken(user.getEmail());

        return ApiResponse.success(
                "Login successful",
                AuthResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build()
        );
    }
}