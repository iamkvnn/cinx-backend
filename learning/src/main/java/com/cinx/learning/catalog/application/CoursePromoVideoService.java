package com.cinx.learning.catalog.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.command.UpdateCoursePromoVideoCommand;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.application.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoursePromoVideoService implements ICoursePromoVideoService {
    private final CourseRepository courseRepository;
    private final ICourseAccessService courseAccessService;
    private final ObjectStorage s3Service;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    @Override
    @Transactional
    public void putPromoVideo(String currentUserId, String courseId, UpdateCoursePromoVideoCommand request) {
        Course course = editableCourse(currentUserId, courseId);
        String replacedKey = course.getPromoVideoKey();

        course.setPromoVideoKey(request.getFileKey());
        course.setPromoVideoUrl(cdnUrl + "/" + request.getFileKey());
        course.setPromoVideoFileName(request.getFileName());
        course.setPromoVideoFileSize(request.getFileSize());
        course.setPromoVideoDuration(request.getDuration());
        courseRepository.save(course);

        if (!Objects.equals(replacedKey, request.getFileKey())) {
            deleteAfterCommit(courseId, replacedKey);
        }
    }

    @Override
    @Transactional
    public void removePromoVideo(String currentUserId, String courseId) {
        Course course = editableCourse(currentUserId, courseId);
        String removedKey = course.getPromoVideoKey();

        course.setPromoVideoKey(null);
        course.setPromoVideoUrl(null);
        course.setPromoVideoFileName(null);
        course.setPromoVideoFileSize(null);
        course.setPromoVideoDuration(null);
        courseRepository.save(course);

        deleteAfterCommit(courseId, removedKey);
    }

    private Course editableCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        return course;
    }

    private void deleteAfterCommit(String courseId, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                int failed = s3Service.deleteObjects(List.of(key));
                if (failed > 0) {
                    log.warn("Course {}: the previous intro video was not deleted from S3", courseId);
                }
            }
        });
    }
}
