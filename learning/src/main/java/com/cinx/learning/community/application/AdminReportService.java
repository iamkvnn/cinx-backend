package com.cinx.learning.community.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.learning.community.domain.CourseAnswer;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.Report;
import com.cinx.learning.community.domain.ReportType;
import com.cinx.learning.community.domain.Review;
import com.cinx.learning.community.domain.CourseAnswerRepository;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.community.domain.ReportRepository;
import com.cinx.learning.community.domain.ReviewRepository;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.community.application.UserSummaryDirectory;
import com.cinx.learning.community.application.query.AdminReportView;
import com.cinx.learning.community.application.query.ReportedContentView;
import com.cinx.learning.community.application.query.UserSummaryView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReportService implements IAdminReportService {
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final CourseQuestionRepository questionRepository;
    private final CourseAnswerRepository answerRepository;
    private final UserSummaryDirectory userClient;
    private final IReviewService reviewService;
    private final ICourseQnAService qnaService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminReportView> getReports(ReportType type, int page, int size, String query, String sort) {
        PageQuery pageable = new PageQuery(page, size, sort);
        PageResult<Report> reports = reportRepository.search(type, normalizeQuery(query), pageable);

        ReportContext context = buildReportContext(reports.content());
        return reports.map(report -> toAdminReportResponse(report, context));
    }

    @Override
    @Transactional
    public void dismissReport(String reportId) {
        reportRepository.deleteById(reportId);
    }

    @Override
    @Transactional
    public void deleteReportedContent(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        switch (report.getType()) {
            case REVIEW -> reviewService.deleteReviewByAdmin(report.getRefId());
            case QUESTION -> qnaService.deleteQuestionByAdmin(report.getRefId());
            case ANSWER -> qnaService.deleteAnswerByAdmin(report.getRefId());
        }
    }

    private ReportContext buildReportContext(List<Report> reports) {
        if (reports.isEmpty()) {
            return new ReportContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        Map<String, Review> reviewsById = loadReviewsById(reports);
        Map<String, CourseQuestion> questionsById = loadQuestionsById(reports);
        Map<String, CourseAnswer> answersById = loadAnswersById(reports);
        Map<String, UserSummaryView> usersById = fetchUsersById(reports, reviewsById.values(), questionsById.values(), answersById.values());

        return new ReportContext(reviewsById, questionsById, answersById, usersById);
    }

    private Map<String, Review> loadReviewsById(List<Report> reports) {
        List<String> ids = refIdsByType(reports, ReportType.REVIEW);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return reviewRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Review::getId, Function.identity()));
    }

    private Map<String, CourseQuestion> loadQuestionsById(List<Report> reports) {
        List<String> ids = refIdsByType(reports, ReportType.QUESTION);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CourseQuestion::getId, Function.identity()));
    }

    private Map<String, CourseAnswer> loadAnswersById(List<Report> reports) {
        List<String> ids = refIdsByType(reports, ReportType.ANSWER);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return answerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CourseAnswer::getId, Function.identity()));
    }

    private List<String> refIdsByType(List<Report> reports, ReportType type) {
        return reports.stream()
                .filter(report -> report.getType() == type)
                .map(Report::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<String, UserSummaryView> fetchUsersById(
            List<Report> reports,
            Collection<Review> reviews,
            Collection<CourseQuestion> questions,
            Collection<CourseAnswer> answers) {
        List<String> userIds = reports.stream()
                .map(Report::getReporterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        reviews.stream().map(Review::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        questions.stream().map(CourseQuestion::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        answers.stream().map(CourseAnswer::getUserId).filter(Objects::nonNull).forEach(userIds::add);

        List<String> distinctUserIds = userIds.stream().distinct().toList();
        if (distinctUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            com.cinx.common.dto.ApiResponse<List<UserSummaryView>> response = userClient.getUsersByIds(distinctUserIds);
            if (response == null || !response.success() || response.data() == null) {
                return Collections.emptyMap();
            }
            return response.data().stream()
                    .filter(user -> user.userId() != null)
                    .collect(Collectors.toMap(UserSummaryView::userId, Function.identity(), (first, second) -> first));
        } catch (Exception ex) {
            log.warn("Failed to fetch admin report user summaries", ex);
            return Collections.emptyMap();
        }
    }

    private AdminReportView toAdminReportResponse(Report report, ReportContext context) {
        return new AdminReportView(
                report.getId(),
                report.getReporterId(),
                context.usersById().get(report.getReporterId()),
                report.getType(),
                report.getReason(),
                toReportedContentResponse(report, context),
                report.getCreatedAt(),
                report.getUpdatedAt());
    }

    private ReportedContentView toReportedContentResponse(Report report, ReportContext context) {
        return switch (report.getType()) {
            case REVIEW -> toReviewContent(report, context);
            case QUESTION -> toQuestionContent(report, context);
            case ANSWER -> toAnswerContent(report, context);
        };
    }

    private ReportedContentView toReviewContent(Report report, ReportContext context) {
        Review review = context.reviewsById().get(report.getRefId());
        if (review == null) {
            return missingContent(report);
        }
        return new ReportedContentView(
                report.getRefId(),
                report.getType(),
                review.getUserId(),
                context.usersById().get(review.getUserId()),
                review.getCourseId(),
                null,
                null,
                null,
                review.getContent(),
                review.getRating(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private ReportedContentView toQuestionContent(Report report, ReportContext context) {
        CourseQuestion question = context.questionsById().get(report.getRefId());
        if (question == null) {
            return missingContent(report);
        }
        return new ReportedContentView(
                report.getRefId(),
                report.getType(),
                question.getUserId(),
                context.usersById().get(question.getUserId()),
                question.getCourseId(),
                question.getLessonId(),
                null,
                question.getTitle(),
                question.getContent(),
                null,
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    private ReportedContentView toAnswerContent(Report report, ReportContext context) {
        CourseAnswer answer = context.answersById().get(report.getRefId());
        if (answer == null) {
            return missingContent(report);
        }
        return new ReportedContentView(
                report.getRefId(),
                report.getType(),
                answer.getUserId(),
                context.usersById().get(answer.getUserId()),
                null,
                null,
                answer.getQuestionId(),
                null,
                answer.getContent(),
                null,
                answer.getCreatedAt(),
                answer.getUpdatedAt());
    }

    private ReportedContentView missingContent(Report report) {
        return new ReportedContentView(
                report.getRefId(),
                report.getType(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private record ReportContext(
            Map<String, Review> reviewsById,
            Map<String, CourseQuestion> questionsById,
            Map<String, CourseAnswer> answersById,
            Map<String, UserSummaryView> usersById) {
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
