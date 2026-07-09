package com.ibnfirnas.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GalleryResponse {
    private Long id;
    private String title;
    private String description;
    private String mediaUrl;
    private String thumbnailUrl;
    private String mediaType;
    private String altText;
    private Integer displayOrder;
    private LocalDateTime createdAt;

}