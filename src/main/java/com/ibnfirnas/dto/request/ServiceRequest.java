package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ServiceRequest {
    @NotBlank(message = "Service name is required")
    private String name;

    private String description;
    private String shortDescription;
    private String imageUrl;
    private String imagePublicId;
    private Boolean isFeatured;
    private Boolean isActive;
    private Integer displayOrder;
}
