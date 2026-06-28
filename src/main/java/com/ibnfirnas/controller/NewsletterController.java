package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.NewsletterRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @Valid @RequestBody NewsletterRequest request) {
        newsletterService.subscribe(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Subscribed successfully", null));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @Valid @RequestBody NewsletterRequest request) {
        newsletterService.unsubscribe(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully", null));
    }
}