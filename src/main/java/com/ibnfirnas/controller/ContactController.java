package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.ContactRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.service.CompanyService;
import com.ibnfirnas.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;
    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> contact(
            @Valid @RequestBody ContactRequest request) {
        String adminEmail = companyService.getCompanyInfo().getEmail();
        emailService.sendContactNotificationToAdmin(adminEmail, request.getName(),
                request.getEmail(), request.getPhone(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.success(
                "Message sent. We will contact you soon.", null));
    }
}