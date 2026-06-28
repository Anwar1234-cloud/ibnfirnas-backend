package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class NewsletterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;
}