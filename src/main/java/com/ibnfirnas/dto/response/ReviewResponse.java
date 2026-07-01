package com.ibnfirnas.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String userFullName;
    private String userAvatar;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean isVerified;
    private LocalDateTime createdAt;

}