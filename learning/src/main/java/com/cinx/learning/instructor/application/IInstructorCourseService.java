package com.cinx.learning.instructor.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.instructor.domain.CourseLifecycle;
import com.cinx.learning.instructor.application.query.CourseLearnersView;
import com.cinx.learning.instructor.application.query.InstructorCourseRow;
import com.cinx.learning.instructor.application.query.LessonReachView;
import com.cinx.learning.instructor.application.query.QuizSummary;

import java.util.List;
import java.util.Set;

/** The instructor's Khoá học pages: the list, one course's row, its learners, lesson reach and quizzes. */
public interface IInstructorCourseService {
    enum CourseSort { DATE, LEARNERS, WAITING, TITLE }

    enum LearnerFilter { ALL, ACTIVE, COMPLETED, INACTIVE }

    enum LearnerSort { JOINED, PROGRESS, LAST_ACTIVITY, SCORE }

    record CourseQuery(String q, Set<CourseLifecycle> lifecycles, Set<CourseMembershipRole> roles, String categoryId,
                       CourseSort sort, boolean ascending, int page, int size) {
    }

    record LearnerQuery(String q, LearnerFilter filter, LearnerSort sort, boolean ascending, int page, int size) {
    }

    PageResult<InstructorCourseRow> courses(String userId, CourseQuery query);

    InstructorCourseRow course(String userId, String courseId);

    CourseLearnersView learners(String userId, String courseId, LearnerQuery query);

    LessonReachView lessonReach(String userId, String courseId);

    List<QuizSummary> quizzes(String userId, String courseId);
}
