package com.ibnfirnas.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String imageUrl;
    private Boolean isSent;
    private LocalDateTime createdAt;
}