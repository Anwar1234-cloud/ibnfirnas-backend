package com.ibnfirnas.service;

import com.ibnfirnas.entity.OtpVerification;
import com.ibnfirnas.entity.enums.OtpPurpose;
import com.ibnfirnas.entity.enums.OtpType;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.repository.OtpVerificationRepository;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    @Value("${twilio.verify-service-sid}")
    private String verifyServiceSid;

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }


    @Transactional
    public void sendEmailOtp(String email, OtpPurpose purpose) {
        otpRepository.deleteByIdentifierAndPurpose(email, purpose);

        String otp = generateOtp();

        otpRepository.save(OtpVerification.builder()
                .identifier(email)
                .otp(otp)
                .otpType(OtpType.EMAIL)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        emailService.sendOtpEmail(email, otp, purpose);
        log.info("Email OTP sent to: {}", email);
    }


    public void sendSmsOtp(String phone, OtpPurpose purpose) {
        try {
            Verification.creator(verifyServiceSid, phone, "sms").create();
            log.info("SMS OTP sent to: {}", phone);
        } catch (Exception e) {
            log.error("SMS OTP failed: {}", e.getMessage());
            throw new BadRequestException("Failed to send SMS OTP: " + e.getMessage());
        }
    }


    @Transactional
    public boolean verifyEmailOtp(String email, String otp, OtpPurpose purpose) {
        OtpVerification otpVerification = otpRepository
                .findByIdentifierAndOtpTypeAndPurposeAndIsUsedFalse(
                        email, OtpType.EMAIL, purpose)
                .orElseThrow(() -> new BadRequestException(
                        "OTP not found or already used"));

        if (otpVerification.getAttempts() >= 3) {
            throw new BadRequestException(
                    "Too many attempts. Please request a new OTP");
        }

        if (otpVerification.isExpired()) {
            throw new BadRequestException("OTP expired. Request a new one");
        }

        if (!otpVerification.getOtp().equals(otp)) {
            otpVerification.setAttempts(otpVerification.getAttempts() + 1);
            otpRepository.save(otpVerification);
            throw new BadRequestException("Invalid OTP");
        }

        otpVerification.setIsUsed(true);
        otpRepository.save(otpVerification);
        return true;
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