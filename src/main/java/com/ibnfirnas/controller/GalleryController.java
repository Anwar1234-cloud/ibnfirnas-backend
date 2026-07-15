package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.GalleryResponse;
import com.ibnfirnas.entity.Gallery;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.GalleryRepository;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryRepository galleryRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryResponse>>> getAll() {
        List<GalleryResponse> items = galleryRepository
                .findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Gallery fetched", items));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GalleryResponse>> create(
            @RequestBody Gallery gallery,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        gallery.setUploadedBy(user);
        Gallery saved = galleryRepository.save(gallery);
        return ResponseEntity.ok(ApiResponse.success("Gallery item added", toResponse(saved)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Gallery gallery = galleryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery item not found"));
        if (gallery.getMediaUrl() != null) {
            cloudinaryService.deleteImage(gallery.getMediaUrl());
        }
        galleryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    private GalleryResponse toResponse(Gallery gallery) {
        return GalleryResponse.builder()
                .id(gallery.getId())
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .mediaUrl(gallery.getMediaUrl())
                .thumbnailUrl(gallery.getThumbnailUrl())
                .mediaType(gallery.getMediaType())
                .altText(gallery.getAltText())
                .displayOrder(gallery.getDisplayOrder())
                .createdAt(gallery.getCreatedAt())
                .build();
    }
}