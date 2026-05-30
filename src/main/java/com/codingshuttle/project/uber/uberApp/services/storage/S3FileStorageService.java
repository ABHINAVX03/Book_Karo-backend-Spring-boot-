package com.codingshuttle.project.uber.uberApp.services.storage;

import com.codingshuttle.project.uber.uberApp.configs.AppStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage.s3", name = "enabled", havingValue = "true")
@Slf4j
public class S3FileStorageService {

    private final AppStorageProperties storageProperties;
    private final S3Client s3Client;

    public S3FileStorageService(AppStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.s3Client = S3Client.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String upload(MultipartFile file, String folderName) {
        try {
            String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                    : ".bin";
            String key = "uberApp/" + folderName + "/" + UUID.randomUUID() + extension;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            String baseUrl = storageProperties.getS3().getPublicBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = String.format("https://%s.s3.%s.amazonaws.com",
                        storageProperties.getS3().getBucket(),
                        storageProperties.getS3().getRegion());
            }
            String url = baseUrl.replaceAll("/$", "") + "/" + key;
            log.info("Stored file in S3 bucket {} key {}", storageProperties.getS3().getBucket(), key);
            return url;
        } catch (Exception e) {
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
    }
}
