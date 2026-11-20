package com.cinx.learning.community.api;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.community.api.mapper.CommunityApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.community.api.dto.request.CreateAnswerRequest;
import com.cinx.learning.community.api.dto.request.CreateQuestionRequest;
import com.cinx.learning.community.api.dto.request.UpdateAnswerRequest;
import com.cinx.learning.community.api.dto.request.UpdateQuestionRequest;
import com.cinx.learning.community.api.dto.response.AnswerDto;
import com.cinx.learning.community.api.dto.response.QuestionDto;
import com.cinx.learning.community.application.ICourseQnAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community/qna")
@RequiredArgsConstructor
public class CourseQnAController {
    private final ICourseQnAService qnaService;
    private final CommunityApiMapper communityApiMapper;

    @PostMapping("/questions")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<QuestionDto>> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Question created successfully", communityApiMapper.toResponse(qnaService.createQuestion(userId, communityApiMapper.toCommand(request)))));
    }

    @GetMapping("/questions")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<PaginatedApiResponse<QuestionDto>> getQuestions(
            @RequestParam String courseId,
            @RequestParam(required = false) String lessonId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "false") boolean unanswered) {
        String userId = AuthenticationUtil.extractUserId();
        PageResult<QuestionDto> questionDtoPage = qnaService.getQuestionsByCourse(courseId, lessonId, userId, page, size, query, sort, unanswered).map(communityApiMapper::toResponse);
        return ResponseEntity.ok(PaginationWrapper.wrap(questionDtoPage));
    }

    @GetMapping("/questions/similar")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getSimilarQuestions(
            @RequestParam String courseId,
            @RequestParam(required = false) String lessonId,
            @RequestParam String text) {
        String userId = AuthenticationUtil.extractUserId();
        List<QuestionDto> questions = qnaService.findSimilarQuestions(courseId, lessonId, text, userId).stream().map(communityApiMapper::toResponse).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Similar questions fetched successfully", questions));
    }

    @GetMapping("/questions/{questionId}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<QuestionDto>> getQuestionById(@PathVariable String questionId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Question details fetched successfully", communityApiMapper.toResponse(qnaService.getQuestionById(questionId, userId))));
    }

    @GetMapping("/questions/{questionId}/answers")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<PaginatedApiResponse<AnswerDto>> getAnswersForQuestion(
            @PathVariable String questionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        String userId = AuthenticationUtil.extractUserId();
        PageResult<AnswerDto> answerDtoPage = qnaService.getAnswersForQuestion(questionId, userId, page, size, sort).map(communityApiMapper::toResponse);
        return ResponseEntity.ok(PaginationWrapper.wrap(answerDtoPage));
    }

    @PutMapping("/questions/{questionId}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<QuestionDto>> updateQuestion(
            @PathVariable String questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Question updated successfully", communityApiMapper.toResponse(qnaService.updateQuestion(userId, questionId, communityApiMapper.toCommand(request)))));
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable String questionId) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.deleteQuestion(userId, questionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question deleted successfully", null));
    }

    @PostMapping("/questions/{questionId}/upvote")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> upvoteQuestion(@PathVariable String questionId) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.upvoteQuestion(userId, questionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question upvoted successfully", null));
    }

    @PostMapping("/questions/{questionId}/report")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> reportQuestion(
            @PathVariable String questionId,
            @Valid @RequestBody com.cinx.learning.community.api.dto.request.CreateQnAReportRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.reportQuestion(userId, questionId, communityApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Question reported successfully", null));
    }

    @PostMapping("/answers")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<AnswerDto>> createAnswer(@Valid @RequestBody CreateAnswerRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer created successfully", communityApiMapper.toResponse(qnaService.createAnswer(userId, request.getQuestionId(), communityApiMapper.toCommand(request)))));
    }

    @PutMapping("/answers/{answerId}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<AnswerDto>> updateAnswer(
            @PathVariable String answerId,
            @Valid @RequestBody UpdateAnswerRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer updated successfully", communityApiMapper.toResponse(qnaService.updateAnswer(userId, answerId, communityApiMapper.toCommand(request)))));
    }

    @DeleteMapping("/answers/{answerId}")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(@PathVariable String answerId) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.deleteAnswer(userId, answerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer deleted successfully", null));
    }

    @PostMapping("/answers/{answerId}/upvote")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> upvoteAnswer(@PathVariable String answerId) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.upvoteAnswer(userId, answerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer upvoted successfully", null));
    }

    @PutMapping("/answers/{answerId}/helpful")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> markAnswerHelpful(@PathVariable String answerId) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.markAnswerHelpful(userId, answerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer marked as helpful", null));
    }

    @DeleteMapping("/answers/{answerId}/helpful")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> unmarkAnswerHelpful(@PathVariable String answerId) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.unmarkAnswerHelpful(userId, answerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer unmarked as helpful", null));
    }

    @PostMapping("/answers/{answerId}/report")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> reportAnswer(
            @PathVariable String answerId,
            @Valid @RequestBody com.cinx.learning.community.api.dto.request.CreateQnAReportRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        qnaService.reportAnswer(userId, answerId, communityApiMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Answer reported successfully", null));
    }

    @GetMapping("/answers/{answerId}/replies")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<PaginatedApiResponse<AnswerDto>> getReplies(
            @PathVariable String answerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        String userId = AuthenticationUtil.extractUserId();
        PageResult<AnswerDto> answerDtoPage = qnaService.getReplies(answerId, userId, page, size, sort).map(communityApiMapper::toResponse);
        return ResponseEntity.ok(PaginationWrapper.wrap(answerDtoPage));
    }
}
