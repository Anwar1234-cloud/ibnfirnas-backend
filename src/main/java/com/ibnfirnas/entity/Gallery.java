package com.ibnfirnas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "gallery")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Gallery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Column(name = "media_url", nullable = false)
    private String mediaUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @Builder.Default
    @Column(name = "media_type")
    private String mediaType = "IMAGE";

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
