package com.cinx.learning.note.application;

import com.cinx.learning.note.application.command.LessonNoteCommand;
import com.cinx.learning.note.application.query.LessonNoteView;

import java.util.List;

public interface ILessonNoteService {
    LessonNoteView create(String userId, String courseId, String lessonId, LessonNoteCommand request);

    List<LessonNoteView> listByLesson(String userId, String courseId, String lessonId);

    /** Every note of the learner in the course, newest first, with the lesson title. */
    List<LessonNoteView> listByCourse(String userId, String courseId);

    LessonNoteView update(String userId, String noteId, LessonNoteCommand request);

    void delete(String userId, String noteId);
}
