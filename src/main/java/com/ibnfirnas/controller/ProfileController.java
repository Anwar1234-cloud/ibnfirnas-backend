package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.UpdateProfileRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.UserResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ============ toDTO ============
    private UserResponse toDTO(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));
        return ResponseEntity.ok(ApiResponse.success("Profile fetched",
                toDTO(user)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        // Update basic fields
        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());

        // Change password if provided
        if (request.getNewPassword() != null
                && !request.getNewPassword().isEmpty()) {
            if (request.getCurrentPassword() == null
                    || request.getCurrentPassword().isEmpty()) {
                throw new BadRequestException(
                        "Current password is required to set new password");
            }
            if (!passwordEncoder.matches(
                    request.getCurrentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                toDTO(userRepository.save(user))));
    }
}