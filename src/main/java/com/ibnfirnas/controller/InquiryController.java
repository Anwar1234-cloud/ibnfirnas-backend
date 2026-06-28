package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.InquiryRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Inquiry;
import com.ibnfirnas.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Inquiry>> submitInquiry(
            @Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Inquiry submitted", inquiryService.submitInquiry(request)));
    }
}
