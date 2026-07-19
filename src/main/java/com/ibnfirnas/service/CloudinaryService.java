package com.ibnfirnas.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ibnfirnas.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public record UploadResult(String url, String publicId) {}

    public UploadResult uploadImage(MultipartFile file, String folder) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ibnfirnas/" + folder,
                            "resource_type", "image",
                            "quality", "auto",
                            "fetch_format", "auto"
                    )
            );
            return new UploadResult(
                    (String) uploadResult.get("secure_url"),
                    (String) uploadResult.get("public_id"));
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Image upload failed");
        }
    }

    /** Preferred path when the public_id is already known (e.g. stored on the entity) — skips URL-parsing. */
    public void deleteByPublicId(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted image: {}", publicId);
        } catch (IOException e) {
            log.error("Cloudinary delete failed: {}", e.getMessage());
        }
    }

    /** Fallback for callers that only have the URL (e.g. the generic delete-by-url upload endpoint). */
    public void deleteImage(String imageUrl) {
        try {
            deleteByPublicId(extractPublicId(imageUrl));
        } catch (IllegalArgumentException e) {
            log.error("Cloudinary delete skipped, not a valid Cloudinary URL: {}", imageUrl);
        }
    }

    private String extractPublicId(String imageUrl) {
        int uploadIndex = imageUrl.indexOf("/upload/");
        if (uploadIndex == -1) {
            throw new IllegalArgumentException("Not a valid Cloudinary URL: " + imageUrl);
        }

        String afterUpload = imageUrl.substring(uploadIndex + "/upload/".length());
        // Strip an optional version segment, e.g. "v1690000000/"
        afterUpload = afterUpload.replaceFirst("^v\\d+/", "");
        // Strip the file extension
        int lastDot = afterUpload.lastIndexOf('.');
        return lastDot == -1 ? afterUpload : afterUpload.substring(0, lastDot);
    }
}
