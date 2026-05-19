package com.codingshuttle.project.uber.uberApp.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.codingshuttle.project.uber.uberApp.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        try {
            validateUpload(file);
            
            log.info("Attempting to upload file to Cloudinary folder: {}", folderName);
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "uberApp/" + folderName,
                            "resource_type", "auto"
                    ));
            
            String url = (String) uploadResult.get("secure_url");
            log.info("File uploaded successfully: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Cloudinary upload failed for folder {}: {}", folderName, e.getMessage());
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage());
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, or PDF files are allowed");
        }
    }
}
