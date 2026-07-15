package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.SendOtpRequest;
import com.ibnfirnas.dto.request.VerifyOtpRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.enums.OtpPurpose;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        OtpPurpose purpose;
        try {
            purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid OTP purpose");
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            otpService.sendEmailOtp(request.getEmail(), purpose);
            return ResponseEntity.ok(ApiResponse.success(
                    "OTP sent to " + maskEmail(request.getEmail()), null));
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            otpService.sendSmsOtp(request.getPhone(), purpose);
            return ResponseEntity.ok(ApiResponse.success(
                    "OTP sent to " + maskPhone(request.getPhone()), null));
        }

        throw new BadRequestException("Email or phone is required");
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        OtpPurpose purpose = OtpPurpose.valueOf(request.getPurpose());

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            boolean result = otpService.verifyEmailOtp(
                    request.getEmail(), request.getOtp(), purpose);
            return ResponseEntity.ok(ApiResponse.success("Email OTP verified", result));
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            boolean result = otpService.verifySmsOtp(
                    request.getPhone(), request.getOtp());
            return ResponseEntity.ok(ApiResponse.success("SMS OTP verified", result));
        }

        throw new BadRequestException("Email or phone is required");
    }


    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }


    private String maskPhone(String phone) {
        if (phone.length() <= 4) return phone;
        return phone.substring(0, 4) + "******" +
                phone.substring(phone.length() - 2);
    }
}
