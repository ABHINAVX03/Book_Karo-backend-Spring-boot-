package com.codingshuttle.project.uber.uberApp.services.storage;

import com.codingshuttle.project.uber.uberApp.configs.AppStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService {

    private final AppStorageProperties storageProperties;

    public String upload(MultipartFile file, String folderName) {
        try {
            String extension = resolveExtension(file);
            String fileName = UUID.randomUUID() + extension;
            Path targetDir = Path.of(storageProperties.getLocal().getDirectory(), "uberApp", folderName);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(fileName);
            Files.write(targetFile, file.getBytes());

            String baseUrl = storageProperties.getLocal().getBaseUrl().replaceAll("/$", "");
            String relativePath = "uberApp/" + folderName + "/" + fileName;
            String url = baseUrl + "/" + relativePath;
            log.info("Stored file locally at {}", targetFile);
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Local file storage failed: " + e.getMessage(), e);
        }
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.'));
        }
        String contentType = file.getContentType();
        if (contentType == null) return ".bin";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".bin";
        };
    }
}
