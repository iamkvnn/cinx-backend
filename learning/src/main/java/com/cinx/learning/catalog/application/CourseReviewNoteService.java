package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.ReviewNoteStatus;
import com.cinx.learning.catalog.domain.ReviewNoteTarget;
import com.cinx.learning.catalog.application.command.ReviewNoteCommand;
import com.cinx.learning.catalog.application.query.ReviewNoteView;
import com.cinx.learning.catalog.domain.CourseReviewNote;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.CourseReviewNoteRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseReviewNoteService implements ICourseReviewNoteService {
    private final CourseReviewNoteRepository noteRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final SectionRepository sectionRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewNoteView> drafts(String courseId) {
        requireCourse(courseId);
        return toResponses(courseId, noteRepository.findAllByCourseIdAndStatusOrderByCreatedAtAsc(courseId, ReviewNoteStatus.DRAFT));
    }

    @Override
    @Transactional
    public ReviewNoteView create(String courseId, ReviewNoteCommand request) {
        requireCourse(courseId);
        CourseReviewNote note = CourseReviewNote.builder()
                .courseId(courseId)
                .status(ReviewNoteStatus.DRAFT)
                .build();
        apply(note, courseId, request);
        return toResponses(courseId, List.of(noteRepository.save(note))).get(0);
    }

    @Override
    @Transactional
    public ReviewNoteView update(String courseId, String noteId, ReviewNoteCommand request) {
        CourseReviewNote note = requireDraft(courseId, noteId);
        apply(note, courseId, request);
        return toResponses(courseId, List.of(noteRepository.save(note))).get(0);
    }

    @Override
    @Transactional
    public void delete(String courseId, String noteId) {
        noteRepository.delete(requireDraft(courseId, noteId));
    }

    @Override
    @Transactional
    public int send(String courseId, List<String> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return 0;
        }
        Set<String> wanted = new HashSet<>(noteIds);
        List<CourseReviewNote> drafts = noteRepository.findAllByCourseIdAndStatusOrderByCreatedAtAsc(courseId, ReviewNoteStatus.DRAFT).stream()
                .filter(note -> wanted.contains(note.getId()))
                .toList();
        if (drafts.size() != wanted.size()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Only draft notes of this course can be sent");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        drafts.forEach(note -> {
            note.setStatus(ReviewNoteStatus.SENT);
            note.setSentAt(now);
        });
        noteRepository.saveAll(drafts);
        return drafts.size();
    }

    @Override
    @Transactional
    public void resolveSent(String courseId) {
        List<CourseReviewNote> sent = noteRepository.findAllByCourseIdAndStatusOrderByCreatedAtAsc(courseId, ReviewNoteStatus.SENT);
        sent.forEach(note -> note.setStatus(ReviewNoteStatus.RESOLVED));
        noteRepository.saveAll(sent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewNoteView> sent(String courseId) {
        return toResponses(courseId, noteRepository.findAllByCourseIdAndStatusOrderByCreatedAtAsc(courseId, ReviewNoteStatus.SENT));
    }

    private void apply(CourseReviewNote note, String courseId, ReviewNoteCommand request) {
        if (request.target() == ReviewNoteTarget.LESSON) {
            if (request.lessonId() == null || request.lessonId().isBlank()
                    || lessonRepository.findByCourseAndStableId(courseId, request.lessonId()).isEmpty()) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "A lesson note needs a lesson of this course");
            }
            note.setLessonId(request.lessonId());
            note.setVideoSecond(request.videoSecond());
        } else {
            note.setLessonId(null);
            note.setVideoSecond(null);
        }
        note.setTarget(request.target());
        note.setContent(request.content().trim());
    }

    private void requireCourse(String courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
    }

    private CourseReviewNote requireDraft(String courseId, String noteId) {
        return noteRepository.findById(noteId)
                .filter(note -> note.getCourseId().equals(courseId) && note.getStatus() == ReviewNoteStatus.DRAFT)
                .orElseThrow(() -> new NotFoundException("Draft note not found"));
    }

    /** Lesson and section titles come from the course's current lessons, draft or published. */
    private List<ReviewNoteView> toResponses(String courseId, List<CourseReviewNote> notes) {
        Map<String, Lesson> lessons = notes.stream()
                .map(CourseReviewNote::getLessonId)
                .filter(Objects::nonNull)
                .distinct()
                .map(stableId -> lessonRepository.findByCourseAndStableId(courseId, stableId).stream().findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Lesson::getStableId, lesson -> lesson, (a, b) -> a));
        Map<String, Section> sections = sectionRepository.findAllById(lessons.values().stream().map(Lesson::getSectionId).distinct().toList())
                .stream().collect(Collectors.toMap(Section::getId, section -> section));
        return notes.stream()
                .map(note -> {
                    Lesson lesson = note.getLessonId() == null ? null : lessons.get(note.getLessonId());
                    return new ReviewNoteView(
                            note.getId(),
                            note.getCourseId(),
                            note.getTarget(),
                            note.getLessonId(),
                            lesson == null ? null : lesson.getTitle(),
                            lesson == null || sections.get(lesson.getSectionId()) == null ? null : sections.get(lesson.getSectionId()).getTitle(),
                            note.getVideoSecond(),
                            note.getContent(),
                            note.getStatus(),
                            note.getCreatedAt() == null ? null : note.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                            note.getSentAt() == null ? null : note.getSentAt().toInstant(ZoneOffset.UTC));
                })
                .toList();
    }
}
