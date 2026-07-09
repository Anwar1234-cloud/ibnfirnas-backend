package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {

    private String fullName;

    private String phone;

    private String avatarUrl;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String currentPassword;

    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;
}