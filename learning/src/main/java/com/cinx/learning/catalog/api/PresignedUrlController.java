package com.cinx.learning.catalog.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PresignedUrlResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.catalog.application.ObjectStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/upload")
public class PresignedUrlController {

    /** Course images, lesson and intro videos, PDF articles and assignment attachments; no SVG or HTML, the bucket is public. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif",
            "video/mp4", "video/webm", "video/quicktime", "video/x-matroska", "video/x-msvideo",
            "application/pdf", "application/zip", "text/plain", "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ObjectStorage s3Service;

    public PresignedUrlController(ObjectStorage s3Service) {
        this.s3Service = s3Service;
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/presigned-url")
    @PreAuthorize("hasAuthority('PERM_learning.curriculum.update.own') or hasAuthority('PERM_learning.course.update.any')")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        // Exact match: the signed Content-Type must equal the header the client sends to S3.
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Unsupported upload content type: " + contentType);
        }

        // Generate a unique object key for course videos / files
        String fileKey = "courses/" + UUID.randomUUID() + "-" + fileName;
        String presignedUrl = s3Service.generatePresignedUrl(fileKey, contentType);

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .fileKey(fileKey)
                .presignedUrl(presignedUrl)
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }
}
