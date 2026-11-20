package com.cinx.learning.note.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.ICourseAccessService;
import com.cinx.learning.catalog.application.ILessonService;
import com.cinx.learning.note.application.command.LessonNoteCommand;
import com.cinx.learning.note.application.query.LessonNoteView;
import com.cinx.learning.note.domain.LessonNote;
import com.cinx.learning.note.domain.LessonNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonNoteService implements ILessonNoteService {
    private final CatalogLookup catalogLookup;
    private final LessonNoteRepository noteRepository;
    private final ILessonService lessonService;
    private final ICourseAccessService courseAccessService;

    @Override
    @Transactional
    public LessonNoteView create(String userId, String courseId, String lessonId, LessonNoteCommand request) {
        // the lesson-content rule, prerequisite lock included: no notes on a lesson the learner cannot open
        lessonService.ensureCanReadLessonContent(userId, courseId, lessonId, null);
        LessonNote note = LessonNote.builder()
                .userId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .content(request.content().trim())
                .positionSeconds(position(courseId, lessonId, request.positionSeconds()))
                .build();
        return toResponse(noteRepository.save(note), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonNoteView> listByLesson(String userId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(userId, courseId, lessonId, null);
        return noteRepository.findByUserIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(userId, courseId, lessonId).stream()
                .map((note) -> toResponse(note, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonNoteView> listByCourse(String userId, String courseId) {
        // the old video-notes list had no course check
        courseAccessService.ensureReadableCourse(userId, courseId);
        Map<String, String> titles = catalogLookup.readableLessons(courseId).stream()
                .collect(Collectors.toMap(LessonInfo::getStableId, LessonInfo::getTitle, (a, b) -> a));
        return noteRepository.findByUserIdAndCourseIdOrderByCreatedAtDesc(userId, courseId).stream()
                .map((note) -> toResponse(note, titles.get(note.getLessonId())))
                .toList();
    }

    @Override
    @Transactional
    public LessonNoteView update(String userId, String noteId, LessonNoteCommand request) {
        LessonNote note = owned(userId, noteId);
        note.setContent(request.content().trim());
        note.setPositionSeconds(position(note.getCourseId(), note.getLessonId(), request.positionSeconds()));
        return toResponse(noteRepository.save(note), null);
    }

    @Override
    @Transactional
    public void delete(String userId, String noteId) {
        noteRepository.delete(owned(userId, noteId));
    }

    private LessonNote owned(String userId, String noteId) {
        LessonNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        if (!note.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "You are not the owner of this note");
        }
        return note;
    }

    /** A position is kept for video lessons only. */
    private Integer position(String courseId, String lessonId, Integer requested) {
        if (requested == null) {
            return null;
        }
        LessonType type = catalogLookup.readableLessonsByStableId(courseId, lessonId).stream()
                .map(LessonInfo::getLessonType)
                .findFirst()
                .orElse(null);
        if (type != LessonType.VIDEO) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "positionSeconds is only allowed on video lessons");
        }
        return requested;
    }

    private static LessonNoteView toResponse(LessonNote note, String lessonTitle) {
        return new LessonNoteView(note.getId(), note.getCourseId(), note.getLessonId(), lessonTitle,
                note.getContent(), note.getPositionSeconds(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
