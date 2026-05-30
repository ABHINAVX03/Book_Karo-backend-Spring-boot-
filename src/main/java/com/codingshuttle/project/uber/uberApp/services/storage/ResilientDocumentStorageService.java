package com.codingshuttle.project.uber.uberApp.services.storage;

import com.codingshuttle.project.uber.uberApp.configs.AppStorageProperties;
import com.codingshuttle.project.uber.uberApp.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientDocumentStorageService {

    private final CloudinaryService cloudinaryService;
    private final LocalFileStorageService localFileStorageService;
    private final Optional<S3FileStorageService> s3FileStorageService;
    private final AppStorageProperties storageProperties;

    public String uploadFile(MultipartFile file, String folderName) {
        try {
            return cloudinaryService.uploadFile(file, folderName);
        } catch (Exception cloudinaryError) {
            log.warn("Cloudinary upload failed for folder {}: {}", folderName, cloudinaryError.getMessage());
        }

        for (String fallback : parseFallbacks()) {
            try {
                return switch (fallback) {
                    case "s3" -> uploadViaS3(file, folderName);
                    case "local" -> localFileStorageService.upload(file, folderName);
                    default -> throw new IllegalStateException("Unknown storage fallback: " + fallback);
                };
            } catch (Exception fallbackError) {
                log.warn("{} storage fallback failed for folder {}: {}", fallback, folderName, fallbackError.getMessage());
            }
        }

        throw new RuntimeException("All document storage providers failed. Please try again later.");
    }

    private String uploadViaS3(MultipartFile file, String folderName) {
        S3FileStorageService s3 = s3FileStorageService.orElseThrow(
                () -> new IllegalStateException("S3 fallback is not configured"));
        return s3.upload(file, folderName);
    }

    private String[] parseFallbacks() {
        return Arrays.stream(storageProperties.getFallbacks().split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
