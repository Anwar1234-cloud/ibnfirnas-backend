package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.UploadResponse;
import com.ibnfirnas.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
        }

        UploadResponse response = cloudinaryService.uploadImage(file, folder);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Image uploaded successfully",
                        response
                )
        );
    }

    @DeleteMapping("/image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @RequestParam("publicId") String publicId) {

        cloudinaryService.deleteImage(publicId);

        return ResponseEntity.ok(
                ApiResponse.success("Image deleted", null)
        );
    }
}