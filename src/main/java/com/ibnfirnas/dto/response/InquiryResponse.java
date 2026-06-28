package com.ibnfirnas.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InquiryResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
}