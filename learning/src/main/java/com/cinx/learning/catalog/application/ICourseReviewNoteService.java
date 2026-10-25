package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.ReviewNoteCommand;
import com.cinx.learning.catalog.application.query.ReviewNoteView;

import java.util.List;

/** Located review notes: drafted by a reviewer, sent with a rejection, resolved when the course is submitted again. */
public interface ICourseReviewNoteService {
    List<ReviewNoteView> drafts(String courseId);

    ReviewNoteView create(String courseId, ReviewNoteCommand request);

    ReviewNoteView update(String courseId, String noteId, ReviewNoteCommand request);

    void delete(String courseId, String noteId);

    /** Marks these drafts of the course SENT; returns how many were sent. */
    int send(String courseId, List<String> noteIds);

    /** Marks the course's SENT notes RESOLVED (the course was submitted again). */
    void resolveSent(String courseId);

    List<ReviewNoteView> sent(String courseId);
}
