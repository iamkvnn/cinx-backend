package com.cinx.learning.catalog.infrastructure.storage;

import com.cinx.learning.catalog.application.ObjectStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Service("catalogS3Service")
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

    public String generatePresignedUrl(String objectKey, String contentType) {
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

    public String readTextObject(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObject(request, ResponseTransformer.toBytes());
        return bytes.asString(StandardCharsets.UTF_8);
    }

    public java.util.Optional<byte[]> readObject(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        try {
            return java.util.Optional.of(s3Client.getObject(request, ResponseTransformer.toBytes()).asByteArray());
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException ex) {
            return java.util.Optional.empty();
        }
    }

    public void uploadTextFile(String objectKey, String content, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromString(content, StandardCharsets.UTF_8));
    }

    /**
     * Deletes the objects in batches of 1000 (the S3 limit) and returns how many S3 reported as not deleted.
     * A failed batch counts all its keys as failed; nothing is thrown, the caller only logs.
     */
    public int deleteObjects(Collection<String> objectKeys) {
        List<String> keys = objectKeys.stream().filter(k -> k != null && !k.isBlank()).distinct().toList();
        int failed = 0;
        for (int from = 0; from < keys.size(); from += 1000) {
            List<String> batch = keys.subList(from, Math.min(keys.size(), from + 1000));
            try {
                DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder()
                                .objects(batch.stream().map(k -> ObjectIdentifier.builder().key(k).build()).toList())
                                .quiet(true)
                                .build())
                        .build());
                failed += response.errors().size();
            } catch (RuntimeException e) {
                failed += batch.size();
            }
        }
        return failed;
    }

    public String publicUrl(String objectKey) {
        return cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
    }
}

