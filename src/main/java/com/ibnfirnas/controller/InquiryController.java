package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.InquiryRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.InquiryResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> submitInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InquiryRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Inquiry submitted",
                inquiryService.submitInquiry(request, user)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Inquiries fetched",
                inquiryService.getMyInquiries(user.getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getAllInquiries() {
        return ResponseEntity.ok(ApiResponse.success("Inquiries fetched",
                inquiryService.getAllInquiries()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InquiryResponse>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry fetched",
                inquiryService.getInquiryById(id)));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<InquiryResponse>> resolve(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry resolved",
                inquiryService.markResolved(id)));
    }
}