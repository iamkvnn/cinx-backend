package com.cinx.user.shared.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PresignedUrlResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.user.shared.application.ObjectStorage;
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
@RequestMapping("/api/v1/profiles/upload")
public class PresignedUrlController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final ObjectStorage objectStorage;

    public PresignedUrlController(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Operation(summary = "Get presigned URL for avatar upload", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/presigned-url")
    @PreAuthorize("hasAuthority('PERM_user.profile.update.self')")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        // Exact match: the signed Content-Type must equal the header the client sends to S3.
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Only PNG, JPEG and WebP images are allowed");
        }

        String fileKey = "avatars/" + UUID.randomUUID() + "-" + fileName;
        String presignedUrl = objectStorage.presignedUploadUrl(fileKey, contentType);

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .fileKey(fileKey)
                .presignedUrl(presignedUrl)
                .build();

        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }
}
