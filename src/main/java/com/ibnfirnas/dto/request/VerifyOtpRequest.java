package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VerifyOtpRequest {

    @Email(message = "Invalid email")
    private String email;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Invalid phone number"
    )
    private String phone;

    @Pattern(
            regexp = "\\d{6}",
            message = "OTP must be exactly 6 digits"
    )
    private String otp;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}