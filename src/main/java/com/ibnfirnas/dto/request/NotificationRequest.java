package com.ibnfirnas.dto.request;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class NotificationRequest {
    private String title;
    private String message;
    private String type;
    private String targetAudience;
    private String imageUrl;
}