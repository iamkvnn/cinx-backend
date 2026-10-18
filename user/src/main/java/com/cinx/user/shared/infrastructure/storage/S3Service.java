package com.cinx.user.shared.infrastructure.storage;

import com.cinx.user.shared.application.ObjectStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class S3Service implements ObjectStorage {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    public S3Service(S3Presigner s3Presigner, S3Client s3Client) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
    }

    @Override
    public String publicUrl(String fileKey) {
        return cdnUrl + "/" + fileKey;
    }

    @Override
    public String presignedUploadUrl(String objectKey, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            // Log and ignore
            System.err.println("Error deleting S3 object {" + objectKey + "}: " + e.getMessage());
        }
    }
}
