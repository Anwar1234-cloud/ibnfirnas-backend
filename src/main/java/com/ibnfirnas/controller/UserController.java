package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.UserResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll()
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.success("User fetched", toDTO(user)));
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<UserResponse>> toggleActive(
            @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        user.setIsActive(!user.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(
                user.getIsActive() ? "User activated" : "User deactivated",
                toDTO(userRepository.save(user))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}