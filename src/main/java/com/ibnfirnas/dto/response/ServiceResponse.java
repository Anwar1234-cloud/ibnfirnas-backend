package com.ibnfirnas.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ServiceResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private String iconUrl;
    private String imageUrl;
    private Boolean isFeatured;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}