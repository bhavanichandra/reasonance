package com.themuler.reasonance.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FileStore {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public FileStore(S3Client s3Client, S3Presigner s3Presigner, @Value("${rustfs.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public void putObject(String key, InputStream inputStream, long contentLength, String contentType) {
        log.debug("Uploading object to S3. Bucket: {}, Key: {}, ContentType: {}", bucketName, key, contentType);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
        log.debug("Successfully uploaded object to S3. Key: {}", key);
    }

    public String getPresignedUrl(String key, Duration duration) {
        log.debug("Generating presigned URL. Bucket: {}, Key: {}, Duration: {}", bucketName, key, duration);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.debug("Generated presigned URL for Key: {}", key);
        return url;
    }

    public String getObjectContent(String key) {
        log.debug("Reading object content from S3. Bucket: {}, Key: {}", bucketName, key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            log.debug("Successfully read object content from S3. Key: {}, Size: {} bytes", key, content.length());
            return content;
        } catch (IOException e) {
            log.error("Failed to read object content from S3. Key: {}", key, e);
            throw new RuntimeException("Failed to read object content from S3", e);
        }
    }

    public void deleteObject(String key) {
        log.debug("Deleting object from S3. Bucket: {}, Key: {}", bucketName, key);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        log.debug("Deleted object from S3. Key: {}", key);
    }
}
