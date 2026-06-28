package com.ibnfirnas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    public String storeFile(MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetPath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath,
                    StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + fileName;
        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage());
            throw new RuntimeException("Could not store file");
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            Path filePath = Paths.get("." + fileUrl);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("File delete failed: {}", e.getMessage());
        }
    }
}
