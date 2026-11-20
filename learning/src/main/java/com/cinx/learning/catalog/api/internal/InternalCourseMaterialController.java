package com.cinx.learning.catalog.api.internal;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.ObjectStorage;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * The bytes of a file an instructor uploaded through the course presigned URL, for the assistant to extract text.
 * The assistant checks that the instructor may edit the course before it asks.
 */
@Hidden
@RestController
@RequestMapping("/internal/courses/materials")
@RequiredArgsConstructor
public class InternalCourseMaterialController {
    // only keys the course upload endpoint makes (courses/<uuid>-<name>); never subtitles, images of others, or paths
    private static final Pattern UPLOAD_KEY = Pattern.compile("^courses/[0-9a-f-]{36}-[^/]{1,200}$");
    private static final int MAX_BYTES = 25 * 1024 * 1024;

    private final ObjectStorage objectStorage;

    @GetMapping(value = "/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAuthority('PERM_learning.internal.material.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-assistant-service')")
    public ResponseEntity<byte[]> getMaterial(@RequestParam("key") String key) {
        if (!UPLOAD_KEY.matcher(key).matches()) {
            throw new BadRequestException("Not a course upload key");
        }
        byte[] content = objectStorage.readObject(key).orElseThrow(() -> new NotFoundException("Material not found"));
        if (content.length > MAX_BYTES) {
            throw new BadRequestException("Material is larger than 25 MB");
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
    }
}
