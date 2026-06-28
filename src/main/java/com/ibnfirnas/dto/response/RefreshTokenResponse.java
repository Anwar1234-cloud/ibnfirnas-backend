package com.ibnfirnas.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
}