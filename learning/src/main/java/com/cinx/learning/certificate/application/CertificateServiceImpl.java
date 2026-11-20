package com.cinx.learning.certificate.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.certificate.domain.CourseCertificateConditions;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.certificate.domain.CourseCertificateConditionsRepository;
import com.cinx.learning.shared.domain.CertificateStatus;
import com.cinx.learning.certificate.application.command.CertificateConditionsCommand;
import com.cinx.learning.certificate.application.query.CertificateConditionsView;
import com.cinx.learning.certificate.application.query.CertificateRequestView;
import com.cinx.learning.certificate.application.query.CertificateRequestView.CertificateChecks;
import com.cinx.learning.courseview.application.query.CourseView;
import com.cinx.learning.courseview.application.query.UserView;
import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.instructor.application.InstructorCourseLookup;
import com.cinx.learning.certificate.application.mapper.CertificateRequestMapper;
import com.cinx.learning.notificationprojection.application.LearningNotifications;
import com.cinx.learning.notificationprojection.application.event.CertificateApprovedEvent;
import com.cinx.learning.notificationprojection.application.event.CertificateRequestedEvent;
import com.cinx.learning.certificate.domain.CertificateRequest;
import com.cinx.learning.certificate.domain.CertificateRequestRepository;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.media.application.FileStorage;
import com.cinx.learning.shared.application.UserDirectory;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements ICertificateService {
    static final int MIN_REASON = 5;
    static final int MAX_REASON = 500;

    private final CertificateRequestRepository certificateRequestRepository;
    private final CertificateRequestMapper certificateRequestMapper;
    private final CourseService courseService;
    private final InstructorCourseLookup courseLookup;
    private final UserDirectory userService;
    private final CertificateGeneratorService certificateGeneratorService;
    private final FileStorage s3Service;
    private final LearningAuthorizationService authorizationService;
    private final LearningNotifications notificationPublisher;
    private final CertificateEligibilityService eligibilityService;
    private final CourseCertificateConditionsRepository conditionsRepository;
    private final IInstructorActivityService activityService;

    @Override
    @Transactional
    public CertificateRequestView applyForCertificate(String userId, String courseId) {
        // a refused request is reopened; a pending or issued one stays as it is
        CertificateRequest refused = certificateRequestRepository.findByUserIdAndCourseId(userId, courseId)
                .map(existing -> {
                    if (existing.getStatus() != CertificateStatus.REJECTED) {
                        throw new BadRequestException(ErrorCode.CERTIFICATE_ALREADY_APPLIED, "You have already applied for a certificate for this course");
                    }
                    return existing;
                })
                .orElse(null);

        CourseView course = courseService.getCourseById(courseId).data();
        if (Boolean.FALSE.equals(course.hasCertificate())) {
            throw new BadRequestException(ErrorCode.CERTIFICATE_NOT_AVAILABLE, "This course does not offer a certificate");
        }

        // the course's own conditions decide, not "every item passed"; a course without conditions uses the defaults
        CourseCertificateConditions conditions = eligibilityService.conditionsOf(courseId);
        CertificateChecks checks = eligibilityService.evaluate(userId, courseId, conditions, eligibilityService.lessonsOf(courseId));
        if (!checks.eligible()) {
            throw new BadRequestException(ErrorCode.COURSE_NOT_COMPLETED, "You have not met this course's certificate conditions yet");
        }

        CertificateRequest request;
        if (refused != null) {
            request = refused;
            request.setStatus(CertificateStatus.PENDING);
            request.setRequestedAt(LocalDateTime.now());
            request.setRejectionReason(null);
            request.setRejectedAt(null);
        } else {
            request = CertificateRequest.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .status(CertificateStatus.PENDING)
                    .requestedAt(LocalDateTime.now())
                    .build();
        }
        CertificateRequest savedRequest = certificateRequestRepository.save(request);
        UserView user = userService.getUserById(userId).data();
        notificationPublisher.publishCertificateRequested(CertificateRequestedEvent.builder()
                .requestId(savedRequest.getId())
                .userId(userId)
                .userName(user != null ? user.name() : null)
                .courseId(courseId)
                .courseTitle(course.title())
                .instructorId(course.instructor() != null ? course.instructor().id() : null)
                .instructorEmail(course.instructor() != null ? course.instructor().email() : null)
                .occurredAt(Instant.now())
                .build());

        if (Boolean.TRUE.equals(conditions.getAutoApprove())) {
            String ownerId = course.instructor() != null ? course.instructor().id() : null;
            CertificateRequest issued = issue(savedRequest, course, user);
            if (ownerId != null) {
                activityService.record(ownerId, courseId, InboxKind.CERTIFICATES, ActivityAction.CERTIFICATE_APPROVED,
                        courseId, 1, course.title() + " · auto");
            }
            return certificateRequestMapper.toDto(issued);
        }
        return certificateRequestMapper.toDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CertificateRequestView> getRequestsByCourse(String currentUserId, String courseId, CertificateStatus status, int page, int size, String query, String sort) {
        authorizationService.requireCourseInstructor(currentUserId, courseId);
        PageResult<CertificateRequest> requests = certificateRequestRepository.search(courseId, status, normalizeQuery(query), new PageQuery(page, size, sort));
        Map<String, CourseChecks> perCourse = new HashMap<>();
        return requests.map(request -> withChecks(request, perCourse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CertificateRequestView> getAllRequests(String currentUserId, CertificateStatus status, int page, int size, String query, String sort) {
        PageQuery pageable = new PageQuery(page, size, sort);
        // only courses the caller manages: approving and rejecting need the same role, so an editor's list would be rows they cannot act on
        List<String> courseIds = courseLookup.managedCourses(currentUserId).stream()
                .map(InstructorCourseLookup.CourseCard::id)
                .toList();
        if (courseIds.isEmpty()) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        Map<String, CourseChecks> perCourse = new HashMap<>();
        return certificateRequestRepository.searchByCourseIds(courseIds, status, normalizeQuery(query), pageable)
                .map(request -> withChecks(request, perCourse));
    }

    @Override
    @Transactional
    public CertificateRequestView approveCertificate(String currentUserId, String requestId) {
        CertificateRequest request = requirePending(currentUserId, requestId);
        CourseView courseDto = courseService.getCourseById(request.getCourseId()).data();
        UserView userDto = userService.getUserById(request.getUserId()).data();
        CertificateRequest saved = issue(request, courseDto, userDto);
        activityService.record(currentUserId, saved.getCourseId(), InboxKind.CERTIFICATES, ActivityAction.CERTIFICATE_APPROVED,
                saved.getCourseId(), 1, courseDto.title());
        return certificateRequestMapper.toDto(saved);
    }

    /** Approves each request on its own: one that fails (not pending, not yours, generation error) does not stop the rest. */
    @Override
    @Transactional
    public CertificateConditionsView.BatchResult approveCertificates(String currentUserId, List<String> requestIds) {
        List<CertificateConditionsView.Result> results = new ArrayList<>();
        Map<String, Integer> approvedPerCourse = new LinkedHashMap<>();
        Map<String, CourseView> courses = new HashMap<>();
        for (String requestId : requestIds.stream().distinct().toList()) {
            try {
                CertificateRequest request = requirePending(currentUserId, requestId);
                CourseView course = courses.computeIfAbsent(request.getCourseId(), id -> courseService.getCourseById(id).data());
                issue(request, course, userService.getUserById(request.getUserId()).data());
                approvedPerCourse.merge(request.getCourseId(), 1, Integer::sum);
                results.add(new CertificateConditionsView.Result(requestId, true, null));
            } catch (RuntimeException e) {
                log.warn("Certificate request {} was not approved: {}", requestId, e.getMessage());
                results.add(new CertificateConditionsView.Result(requestId, false, e.getMessage()));
            }
        }
        approvedPerCourse.forEach((courseId, count) -> activityService.record(currentUserId, courseId, InboxKind.CERTIFICATES,
                ActivityAction.CERTIFICATE_APPROVED, courseId, count, courses.get(courseId).title()));
        return new CertificateConditionsView.BatchResult(results);
    }

    @Override
    @Transactional
    public CertificateRequestView rejectCertificate(String currentUserId, String requestId, String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.length() < MIN_REASON || trimmed.length() > MAX_REASON) {
            throw new BadRequestException(ErrorCode.REJECTION_REASON_REQUIRED, "A reason of 5 to 500 characters is required");
        }
        CertificateRequest request = requirePending(currentUserId, requestId);
        request.setStatus(CertificateStatus.REJECTED);
        request.setRejectionReason(trimmed);
        request.setRejectedAt(LocalDateTime.now());
        CertificateRequest saved = certificateRequestRepository.save(request);
        activityService.record(currentUserId, saved.getCourseId(), InboxKind.CERTIFICATES, ActivityAction.CERTIFICATE_REJECTED,
                saved.getCourseId(), 1, trimmed);
        return certificateRequestMapper.toDto(saved);
    }

    @Override
    public CertificateRequestView getCertificate(String userId, String courseId) {
        return certificateRequestRepository.findByUserIdAndCourseId(userId, courseId)
                .map(certificateRequestMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Certificate not found for this user and course"));
    }

    @Override
    public List<CertificateRequestView> getMyCertificates(String userId) {
        return certificateRequestRepository.findByUserIdAndStatus(userId, CertificateStatus.APPROVED)
                .stream()
                .map(certificateRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateConditionsView getConditions(String currentUserId, String courseId) {
        authorizationService.requireCourseInstructor(currentUserId, courseId);
        return toResponse(eligibilityService.conditionsOf(courseId));
    }

    /**
     * Saves the conditions and re-checks the course's pending requests; with auto-approve on, those that now pass are
     * approved at once, as the caller.
     */
    @Override
    @Transactional
    public CertificateConditionsView.Saved saveConditions(String currentUserId, String courseId, CertificateConditionsCommand request) {
        authorizationService.requireCourseInstructor(currentUserId, courseId);
        CourseCertificateConditions conditions = conditionsRepository.findByCourseId(courseId)
                .orElseGet(() -> CourseCertificateConditions.builder().courseId(courseId).build());
        conditions.setMinCompletionPercent(request.minCompletionPercent());
        conditions.setMinQuizScore(request.minQuizScore());
        conditions.setMinAssignmentScore(request.minAssignmentScore());
        conditions.setAutoApprove(request.autoApprove());
        CourseCertificateConditions saved = conditionsRepository.saveAndFlush(conditions);

        int approved = 0;
        if (Boolean.TRUE.equals(saved.getAutoApprove())) {
            List<LessonInfo> lessons = eligibilityService.lessonsOf(courseId);
            CourseView course = null;
            for (CertificateRequest pending : certificateRequestRepository.findAllByCourseIdAndStatus(courseId, CertificateStatus.PENDING)) {
                if (!eligibilityService.evaluate(pending.getUserId(), courseId, saved, lessons).eligible()) {
                    continue;
                }
                course = course != null ? course : courseService.getCourseById(courseId).data();
                issue(pending, course, userService.getUserById(pending.getUserId()).data());
                approved++;
            }
            if (approved > 0) {
                activityService.record(currentUserId, courseId, InboxKind.CERTIFICATES, ActivityAction.CERTIFICATE_APPROVED,
                        courseId, approved, course.title() + " · auto");
            }
        }
        activityService.record(currentUserId, courseId, InboxKind.CERTIFICATES, ActivityAction.CONDITIONS_CHANGED, courseId, 1,
                saved.getMinCompletionPercent() + "% · quiz " + saved.getMinQuizScore() + " · assignment " + saved.getMinAssignmentScore());
        return new CertificateConditionsView.Saved(toResponse(saved), approved);
    }

    private CertificateRequest requirePending(String currentUserId, String requestId) {
        CertificateRequest request = certificateRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Certificate request not found"));
        authorizationService.requireCourseInstructor(currentUserId, request.getCourseId());
        if (request.getStatus() != CertificateStatus.PENDING) {
            throw new BadRequestException(ErrorCode.CERTIFICATE_REQUEST_NOT_PENDING, "Request is not in PENDING state");
        }
        return request;
    }

    /** Generates and uploads the certificate, marks the request approved and tells the learner. */
    private CertificateRequest issue(CertificateRequest request, CourseView courseDto, UserView userDto) {
        String title = courseDto.certificateTitle() != null ? courseDto.certificateTitle() : "Certificate of Completion";
        byte[] certificateImage = certificateGeneratorService.generateCertificate(userDto.name(), courseDto.title(), title);

        String objectKey = "certificates/" + request.getCourseId() + "/" + UUID.randomUUID() + ".png";
        String uploadedUrl = s3Service.uploadFile(objectKey, certificateImage, "image/png");

        request.setStatus(CertificateStatus.APPROVED);
        request.setCertificateUrl(uploadedUrl);
        request.setApprovedAt(LocalDateTime.now());

        CertificateRequest savedRequest = certificateRequestRepository.save(request);
        notificationPublisher.publishCertificateApproved(CertificateApprovedEvent.builder()
                .requestId(savedRequest.getId())
                .userId(savedRequest.getUserId())
                .userName(userDto.name())
                .userEmail(userDto.email())
                .courseId(savedRequest.getCourseId())
                .courseTitle(courseDto.title())
                .certificateUrl(uploadedUrl)
                .occurredAt(Instant.now())
                .build());
        return savedRequest;
    }

    private record CourseChecks(CourseCertificateConditions conditions, List<LessonInfo> lessons) {
    }

    private CertificateRequestView withChecks(CertificateRequest request, Map<String, CourseChecks> perCourse) {
        CertificateRequestView dto = certificateRequestMapper.toDto(request);
        if (request.getStatus() != CertificateStatus.PENDING) {
            return dto;
        }
        CourseChecks course = perCourse.computeIfAbsent(request.getCourseId(),
                id -> new CourseChecks(eligibilityService.conditionsOf(id), eligibilityService.lessonsOf(id)));
        return dto.withChecks(eligibilityService.evaluate(request.getUserId(), request.getCourseId(), course.conditions(), course.lessons()));
    }

    private static CertificateConditionsView toResponse(CourseCertificateConditions c) {
        return new CertificateConditionsView(
                c.getCourseId(),
                c.getMinCompletionPercent(),
                c.getMinQuizScore(),
                c.getMinAssignmentScore(),
                Boolean.TRUE.equals(c.getAutoApprove()),
                c.getUpdatedAt() == null ? null : c.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
