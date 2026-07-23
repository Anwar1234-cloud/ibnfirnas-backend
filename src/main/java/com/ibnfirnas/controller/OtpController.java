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

        OtpPurpose purpose = parsePurpose(request.getPurpose());
        otpService.sendSmsOtp(request.getPhone(), purpose);
        return ResponseEntity.ok(ApiResponse.success(
                "OTP sent to " + maskPhone(request.getPhone()), null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        parsePurpose(request.getPurpose());
        boolean result = otpService.verifySmsOtp(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("SMS OTP verified", result));
    }

    private OtpPurpose parsePurpose(String purpose) {
        try {
            return OtpPurpose.valueOf(purpose.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid OTP purpose");
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) return phone;
        return phone.substring(0, 4) + "******" +
                phone.substring(phone.length() - 2);
    }
}
