package com.ibnfirnas.service;

import com.ibnfirnas.entity.enums.OtpPurpose;
import com.ibnfirnas.exception.BadRequestException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    @Value("${twilio.verify-service-sid}")
    private String verifyServiceSid;

    public void sendSmsOtp(String phone, OtpPurpose purpose) {
        try {
            Verification.creator(verifyServiceSid, phone, "sms").create();
            log.info("SMS OTP sent to: {}", phone);
        } catch (Exception e) {
            log.error("SMS OTP failed: {}", e.getMessage());
            throw new BadRequestException("Failed to send SMS OTP: " + e.getMessage());
        }
    }

    public boolean verifySmsOtp(String phone, String otp) {
        try {
            VerificationCheck check = VerificationCheck.creator(verifyServiceSid)
                    .setTo(phone)
                    .setCode(otp)
                    .create();

            if ("approved".equals(check.getStatus())) {
                log.info("SMS OTP verified for: {}", phone);
                return true;
            } else {
                throw new BadRequestException("Invalid OTP");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("SMS OTP verification failed: {}", e.getMessage());
            throw new BadRequestException("OTP verification failed");
        }
    }
}
