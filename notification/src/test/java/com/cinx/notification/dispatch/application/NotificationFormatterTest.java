package com.cinx.notification.dispatch.application;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationFormatterTest {

    private final NotificationFormatter formatter = new NotificationFormatter();

    @Test
    void coursePublishedForLearnersBuildsVietnamesePayloadWithAction() {
        NotificationMessage message = formatter.coursePublishedForLearners("course-1", "Java <Basic>");

        assertThat(message.type()).isEqualTo("COURSE_CONTENT_PUBLISHED");
        assertThat(message.title()).isEqualTo("Nội dung khóa học mới");
        assertThat(message.message()).contains("\"Java <Basic>\"");
        assertThat(message.actionUrl()).isEqualTo("/courses/course-1");
        assertThat(message.metadata()).containsEntry("courseId", "course-1");
        assertThat(message.htmlBody()).contains("Java &lt;Basic&gt;");
        assertThat(message.inAppPayload(List.of("user-1")))
                .containsEntry("type", "COURSE_CONTENT_PUBLISHED")
                .containsEntry("referenceId", "course-1")
                .containsEntry("actionUrl", "/courses/course-1");
    }

    @Test
    void dailyReminderBuildsPushData() {
        NotificationMessage message = formatter.dailyReminder("XP", 50, 10, "lesson-1");

        assertThat(message.title()).isEqualTo("Đừng để mất nhịp học");
        assertThat(message.message()).contains("50 XP");
        assertThat(message.pushPayload(List.of("user-1")).get("data"))
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("type", "DAILY_LEARNING_REMINDER")
                .containsEntry("targetItemId", "lesson-1");
    }

    @Test
    void campusEventScheduledNamesTheZoneTheEventAndItsLocalTime() {
        NotificationMessage message = formatter.campusEventScheduled("ev-1", "z-1", "Khu của cô Lan", "Quiz <React>",
                "quiz", Instant.parse("2026-11-13T13:00:00Z"));

        assertThat(message.type()).isEqualTo("CAMPUS_EVENT_SCHEDULED");
        assertThat(message.message())
                .isEqualTo("Khu của cô Lan vừa lên lịch quiz \"Quiz <React>\" lúc 20:00 thứ Sáu 13/11. Bật nhắc để không bỏ lỡ nhé.");
        assertThat(message.actionUrl()).isEqualTo("/campus?zone=z-1");
        assertThat(message.htmlBody()).contains("Quiz &lt;React&gt;");
        assertThat(message.metadata()).containsEntry("startsAt", "2026-11-13T13:00:00Z").containsEntry("kind", "quiz");
    }

    @Test
    void campusEventReminderAndCancellationKeepTheVietnamClockAcrossMidnight() {
        // 17:30 UTC is 00:30 the next day in Vietnam
        Instant startsAt = Instant.parse("2026-11-15T17:30:00Z");

        assertThat(formatter.campusEventReminder("ev-1", "z-1", "Khu của thầy Minh", "Hỏi đáp AI", "meetup", startsAt).message())
                .isEqualTo("Buổi giao lưu \"Hỏi đáp AI\" ở Khu của thầy Minh bắt đầu lúc 00:30 thứ Hai 16/11. Vào khu thôi!");
        NotificationMessage cancelled = formatter.campusEventCancelled("ev-1", "z-1", "Khu của thầy Minh", "Hỏi đáp AI", startsAt);
        assertThat(cancelled.type()).isEqualTo("CAMPUS_EVENT_CANCELLED");
        assertThat(cancelled.message()).isEqualTo("Sự kiện \"Hỏi đáp AI\" ở Khu của thầy Minh lúc 00:30 thứ Hai 16/11 đã bị hủy.");
    }

    @Test
    void campusThreadReplyNamesTheReplierAndTheThreadAndLinksToIt() {
        NotificationMessage message = formatter.campusThreadReply("t 1", "r-1", "zone", "z-1", "Lớp Thảo", "Hỏi về <JOIN>", "Minh");

        assertThat(message.type()).isEqualTo("CAMPUS_THREAD_REPLY");
        assertThat(message.message()).isEqualTo("Minh vừa trả lời bài \"Hỏi về <JOIN>\" của bạn ở Lớp Thảo.");
        assertThat(message.actionUrl()).isEqualTo("/campus?zone=z-1&thread=t+1");
        assertThat(message.htmlBody()).contains("Hỏi về &lt;JOIN&gt;");
        assertThat(message.metadata()).containsEntry("threadId", "t 1").containsEntry("placeKind", "zone").containsEntry("replyId", "r-1");
        assertThat(formatter.campusThreadReply("t-1", "r-1", "club", "c-1", null, "Ôn SQL", "Lan").message())
                .isEqualTo("Lan vừa trả lời bài \"Ôn SQL\" của bạn trên bảng tin Campus.");
        assertThat(formatter.campusThreadReply("t-1", "r-1", "club", "c-1", " ", " ", null).message())
                .isEqualTo("Một bạn học vừa trả lời bài viết của bạn trên bảng tin Campus.");
    }
}
