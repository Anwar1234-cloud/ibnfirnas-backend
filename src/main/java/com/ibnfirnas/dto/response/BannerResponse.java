package com.ibnfirnas.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BannerResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String ctaText;
    private String ctaLink;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}