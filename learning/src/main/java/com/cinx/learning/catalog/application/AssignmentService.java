package com.cinx.learning.catalog.application;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.command.CreateAssignmentLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateAssignmentLessonCommand;
import com.cinx.learning.catalog.application.query.AssignmentLessonView;
import com.cinx.learning.catalog.application.mapper.AssignmentMapper;
import com.cinx.learning.catalog.domain.AssignmentAttachmentRepository;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignmentService implements IAssignmentService {
    private final AssignmentLessonRepository assignmentLessonRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final AssignmentMapper assignmentMapper;
    private final ILessonService lessonService;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    @Override
    public AssignmentLessonView getAssignmentByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.ASSIGNMENT);
        return assignmentLessonRepository.findByLessonId(lessonId)
                .map(assignmentMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Assignment not found for lessonId: " + lessonId));
    }

    @Override
    public void createAssignment(String currentUserId, String courseId, String lessonId, CreateAssignmentLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.ASSIGNMENT);
        assignmentLessonRepository.findByLessonId(lessonId).ifPresentOrElse(existing -> {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Assignment already exists for lessonId: " + lessonId);
        },() -> {
            var assignmentLesson = assignmentMapper.toModel(request);
            assignmentLesson.setLessonId(lessonId);
            if (assignmentLesson.getAttachments() != null) {
                assignmentLesson.getAttachments().forEach(attachment -> {
                    attachment.setAttachmentUrl(cdnUrl + "/" + attachment.getFileKey());
                    attachment.setAssignmentLessonId(assignmentLesson.getLessonId());
                });
            }
            assignmentLessonRepository.save(assignmentLesson);
        });
    }

    @Override
    public void updateAssignment(String currentUserId, String courseId, String lessonId, UpdateAssignmentLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.ASSIGNMENT);
        assignmentLessonRepository.findByLessonId(lessonId).ifPresentOrElse(existing -> {
            assignmentMapper.partialUpdate(existing, request);
            if (existing.getAttachments() != null && !existing.getAttachments().isEmpty()) {
                assignmentAttachmentRepository.deleteAll(existing.getAttachments());
            }
            assignmentLessonRepository.save(existing);
            if (existing.getAttachments() != null) {
                assignmentAttachmentRepository.saveAll(existing.getAttachments().stream().peek(attachment -> {
                    attachment.setAttachmentUrl(cdnUrl + "/" + attachment.getFileKey());
                    attachment.setAssignmentLessonId(existing.getLessonId());
                }).toList());
            }
        },() -> {
            throw new NotFoundException("Assignment not found for lessonId: " + lessonId);
        });
    }
}

