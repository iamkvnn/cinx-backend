package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.CourseTranscriptsView;

public interface ITranscriptService {
    /** Only a PUBLISHED course's published lessons; any other course answers with no lessons. */
    CourseTranscriptsView publishedTranscripts(String courseId);
}
