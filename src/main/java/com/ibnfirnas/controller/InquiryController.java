package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.InquiryRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.InquiryResponse;
import com.ibnfirnas.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> submitInquiry(
            @Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry submitted",
                inquiryService.submitInquiry(request)));
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