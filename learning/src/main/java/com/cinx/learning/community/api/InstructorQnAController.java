package com.cinx.learning.community.api;

import com.cinx.learning.community.api.mapper.CommunityApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.community.api.dto.request.BatchAnswerRequest;
import com.cinx.learning.community.api.dto.response.InstructorQnAResponses;
import com.cinx.learning.community.application.IInstructorQnAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The inbox questions tool: a lesson's unanswered questions, your earlier answers there, and answering together. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/qna")
public class InstructorQnAController {
    private final IInstructorQnAService instructorQnAService;
    private final CommunityApiMapper communityApiMapper;

    @Operation(summary = "A lesson's unanswered questions, most shared first", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/instructor/lessons/{lessonId}/questions")
    public ResponseEntity<ApiResponse<List<InstructorQnAResponses.Question>>> unanswered(@PathVariable String lessonId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("", instructorQnAService.unansweredInLesson(userId, lessonId).stream().map(communityApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Your instructor answers in a lesson, newest first", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/instructor/lessons/{lessonId}/my-answers")
    public ResponseEntity<ApiResponse<List<InstructorQnAResponses.PastAnswer>>> myAnswers(@PathVariable String lessonId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("", instructorQnAService.myAnswersInLesson(userId, lessonId).stream().map(communityApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Post one instructor answer to several questions", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/answers/batch")
    public ResponseEntity<ApiResponse<InstructorQnAResponses.BatchResult>> answerTogether(@Valid @RequestBody BatchAnswerRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Answers posted", communityApiMapper.toResponse(instructorQnAService.answerTogether(userId, communityApiMapper.toCommand(request)))));
    }
}
