package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Gallery;
import com.ibnfirnas.repository.GalleryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryRepository galleryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Gallery>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success("Gallery fetched",
                        galleryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()));
    }
}