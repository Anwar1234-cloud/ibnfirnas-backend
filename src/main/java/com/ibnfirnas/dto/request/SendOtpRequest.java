package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SendOtpRequest {

    @Email(message = "Invalid email")
    private String email;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Invalid phone number"
    )
    private String phone;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}