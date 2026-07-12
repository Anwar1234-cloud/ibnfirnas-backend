package com.ibnfirnas.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

    public String uploadImage(MultipartFile file, String folder) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ibnfirnas/" + folder,
                            "resource_type", "auto",
                            "quality", "auto",
                            "fetch_format", "auto"
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Image upload failed");
        }
    }

    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted image: {}", publicId);
        } catch (IOException e) {
            log.error("Cloudinary delete failed: {}", e.getMessage());
        }
    }

    private String extractPublicId(String imageUrl) {

        String[] parts = imageUrl.split("/");
        String filename = parts[parts.length - 1];
        String folder = parts[parts.length - 2];
        String parentFolder = parts[parts.length - 3];
        return parentFolder + "/" + folder + "/" + filename.split("\\.")[0];
    }
}
