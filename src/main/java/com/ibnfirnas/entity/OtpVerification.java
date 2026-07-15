package com.ibnfirnas.entity;

import com.ibnfirnas.entity.enums.OtpPurpose;
import com.ibnfirnas.entity.enums.OtpType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String otp;

    @Enumerated(EnumType.STRING)
    private OtpType otpType;

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    @Builder.Default
    private Boolean isUsed = false;

    @Builder.Default
    private Integer attempts = 0;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}