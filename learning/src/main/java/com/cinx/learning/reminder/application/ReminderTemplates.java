package com.cinx.learning.reminder.application;

import com.cinx.learning.reminder.domain.ReminderReason;

import java.util.LinkedHashMap;
import java.util.Map;

/** The Vietnamese text of each reason, filled with its facts; what goes out when the assistant cannot rewrite it. */
final class ReminderTemplates {
    static final String WEEK_URL = "/week";
    static final String REVIEW_URL = "/review";

    private ReminderTemplates() {
    }

    static ReminderMessage planSession(String courseId, String courseTitle, String lessonId, String lessonTitle, int minutes) {
        return new ReminderMessage(ReminderReason.PLAN_SESSION_PENDING, "Đến giờ học rồi",
                "Hôm nay bạn có " + minutes + " phút cho «" + lessonTitle + "» (" + courseTitle + "). Học ngay nhé?",
                lessonUrl(courseId, lessonId), facts("courseTitle", courseTitle, "lessonTitle", lessonTitle, "minutes", minutes));
    }

    static ReminderMessage streakAtRisk(int streakDays, String courseId, String lessonId, String lessonTitle) {
        String body = "Bạn đã học " + streakDays + " ngày liền. "
                + (lessonTitle == null ? "Học một bài ngắn hôm nay để giữ chuỗi nhé."
                : "Học tiếp «" + lessonTitle + "» hôm nay để giữ chuỗi nhé.");
        return new ReminderMessage(ReminderReason.STREAK_AT_RISK, "Giữ chuỗi " + streakDays + " ngày nhé", body,
                courseId == null ? WEEK_URL : lessonUrl(courseId, lessonId),
                facts("streakDays", streakDays, "lessonTitle", lessonTitle));
    }

    static ReminderMessage reviewsDue(int questions) {
        return new ReminderMessage(ReminderReason.REVIEWS_DUE, questions + " câu cần ôn hôm nay",
                "Ôn lại " + questions + " câu bạn từng làm chưa đúng, vài phút là xong.", REVIEW_URL,
                facts("questions", questions));
    }

    static ReminderMessage dailyGoal(String goalType, int remaining) {
        String unit = switch (goalType) {
            case "XP" -> "XP";
            case "VIDEOS_COMPLETED" -> "video";
            case "QUIZZES_PASSED" -> "quiz";
            case "ASSIGNMENTS_SUBMITTED" -> "bài tập";
            default -> "bài";
        };
        return new ReminderMessage(ReminderReason.DAILY_GOAL_PENDING, "Mục tiêu hôm nay còn dang dở",
                "Bạn còn " + remaining + " " + unit + " nữa là xong mục tiêu hôm nay.", WEEK_URL,
                facts("remaining", remaining, "unit", unit));
    }

    static ReminderMessage inactive(int daysAway, String courseId, String courseTitle, String lessonId, String lessonTitle) {
        return new ReminderMessage(ReminderReason.INACTIVE, "Lâu rồi chưa gặp bạn",
                "Đã " + daysAway + " ngày bạn chưa học «" + courseTitle + "». Quay lại với «" + lessonTitle + "» nhé?",
                lessonUrl(courseId, lessonId), facts("daysAway", daysAway, "courseTitle", courseTitle, "lessonTitle", lessonTitle));
    }

    private static String lessonUrl(String courseId, String lessonId) {
        return "/learning/" + courseId + (lessonId == null ? "" : "?lessonId=" + lessonId);
    }

    private static Map<String, Object> facts(Object... pairs) {
        Map<String, Object> facts = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i + 1] != null) {
                facts.put((String) pairs[i], pairs[i + 1]);
            }
        }
        return facts;
    }
}
