package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SendOtpRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Invalid phone number"
    )
    private String phone;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}
