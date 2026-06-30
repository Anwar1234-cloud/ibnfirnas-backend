package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.DeviceToken;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.DeviceTokenRepository;
import com.ibnfirnas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-token")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @RequestParam String token,
            @RequestParam String platform,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        deviceTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existing -> {
                            existing.setUser(user);
                            existing.setIsActive(true);
                            deviceTokenRepository.save(existing);
                        },
                        () -> deviceTokenRepository.save(
                                DeviceToken.builder()
                                        .user(user)
                                        .token(token)
                                        .platform(platform)
                                        .build())
                );

        return ResponseEntity.ok(ApiResponse.success("Device token registered", null));
    }
}