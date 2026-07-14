package com.ibnfirnas.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
}