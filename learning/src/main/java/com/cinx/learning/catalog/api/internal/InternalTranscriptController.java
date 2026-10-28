package com.cinx.learning.catalog.api.internal;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.api.dto.response.CourseTranscriptsResponse;
import com.cinx.learning.catalog.application.ITranscriptService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Transcript text for the assistant's index; it re-reads a course on each learning.course.transcripts.changed. */
@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalTranscriptController {
    private final ITranscriptService transcriptService;

    @GetMapping("/courses/{courseId}/transcripts")
    @PreAuthorize("hasAuthority('PERM_learning.internal.transcript.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-assistant-service')")
    public ApiResponse<CourseTranscriptsResponse> getPublishedTranscripts(@PathVariable String courseId) {
        return ApiResponse.success("Transcripts fetched", CourseTranscriptsResponse.from(transcriptService.publishedTranscripts(courseId)));
    }
}
