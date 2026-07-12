package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
        }

        String url = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(
                ApiResponse.success("Image uploaded successfully",
                        Map.of("url", url)));
    }

    @DeleteMapping("/image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @RequestParam("url") String imageUrl) {
        cloudinaryService.deleteImage(imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Image deleted", null));
    }
}