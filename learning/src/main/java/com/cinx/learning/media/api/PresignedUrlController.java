package com.cinx.learning.media.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PresignedUrlResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.media.application.FileStorage;
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
@RequestMapping("/api/v1/learning/upload")
public class PresignedUrlController {

    /** Assignment submissions accept PDF and images only; the player filters the same set. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/webp", "image/gif");

    private final FileStorage s3Service;

    public PresignedUrlController(FileStorage s3Service) {
        this.s3Service = s3Service;
    }

    @Operation(summary = "", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/presigned-url")
    @PreAuthorize("hasAuthority('PERM_learning.progress.write.self')")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        // Exact match: the signed Content-Type must equal the header the client sends to S3.
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Only PDF and image uploads are allowed");
        }

        // Generate a unique object key for learning attachments
        String fileKey = "learning/" + UUID.randomUUID() + "-" + fileName;
        String presignedUrl = s3Service.generatePresignedUrl(fileKey, contentType);

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .fileKey(fileKey)
                .presignedUrl(presignedUrl)
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }
}