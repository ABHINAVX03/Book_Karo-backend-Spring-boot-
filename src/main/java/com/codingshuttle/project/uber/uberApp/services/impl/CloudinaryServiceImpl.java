package com.codingshuttle.project.uber.uberApp.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.codingshuttle.project.uber.uberApp.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        try {
            if (file.isEmpty()) throw new RuntimeException("File is empty");
            
            log.info("Attempting to upload file to Cloudinary folder: {}", folderName);
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "uberApp/" + folderName));
            
            String url = (String) uploadResult.get("secure_url");
            log.info("File uploaded successfully: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Cloudinary upload failed for folder {}: {}", folderName, e.getMessage());
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage());
        }
    }
}
