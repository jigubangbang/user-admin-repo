package com.jigubangbang.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.sync.RequestBody;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String regionString;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        Region region = Region.of(regionString);
        this.s3Client = S3Client.builder()
                .region(region)
                .build();
    }

    public String uploadFile(MultipartFile file, String s3Folder) throws IOException, S3Exception {
        String originalFilename = file.getOriginalFilename();
        if (!s3Folder.endsWith("/")) {
            s3Folder += "/";
        }

        String s3Key = s3Folder + UUID.randomUUID().toString() + "_" + originalFilename;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        System.out.println("S3 파일 업로드 성공. 객체 키: " + s3Key);

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, regionString, s3Key);
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
