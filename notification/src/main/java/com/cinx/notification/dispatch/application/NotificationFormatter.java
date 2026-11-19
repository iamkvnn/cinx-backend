package com.cinx.notification.dispatch.application;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationFormatter {

    // campus event times are told on the learners' clock
    private static final ZoneId CAMPUS_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final String FRIENDS_ACTION = "/user-profile?tab=friends";

    public NotificationMessage coursePublishedForInstructor(String courseId, String courseTitle, String instructorName) {
        String title = "Khóa học đã được xuất bản";
        String safeCourseTitle = defaultText(courseTitle, "khóa học của bạn");
        String name = defaultText(instructorName, "giảng viên");
        return message(
                "COURSE_PUBLISHED",
                title,
                "Khóa học \"" + safeCourseTitle + "\" đã được duyệt và xuất bản.",
                title + " - " + safeCourseTitle,
                paragraphs(
                        "Xin chào " + name + ",",
                        "Khóa học \"" + safeCourseTitle + "\" đã được quản trị viên duyệt và hiện đã được xuất bản.",
                        "Người học đã có thể truy cập nội dung mới nhất của khóa học."
                ),
                courseId,
                courseAction(courseId),
                metadata("courseId", courseId, "courseTitle", safeCourseTitle)
        );
    }

    public NotificationMessage coursePublishedForLearners(String courseId, String courseTitle) {
        String safeCourseTitle = defaultText(courseTitle, "khóa học bạn đang theo dõi");
        return message(
                "COURSE_CONTENT_PUBLISHED",
                "Nội dung khóa học mới",
                "Khóa học \"" + safeCourseTitle + "\" vừa có nội dung mới. Vào học ngay nhé.",
                "Nội dung mới - " + safeCourseTitle,
                paragraphs(
                        "Khóa học \"" + safeCourseTitle + "\" vừa có nội dung mới.",
                        "Hãy quay lại CINX để tiếp tục lộ trình học của bạn."
                ),
                courseId,
                courseAction(courseId),
                metadata("courseId", courseId, "courseTitle", safeCourseTitle)
        );
    }

    public NotificationMessage courseApprovalRequested(String courseId, String courseTitle, String instructorName) {
        String safeCourseTitle = defaultText(courseTitle, "một khóa học");
        String safeInstructorName = defaultText(instructorName, "Một giảng viên");
        return message(
                "COURSE_APPROVAL_REQUESTED",
                "Khóa học chờ duyệt",
                safeInstructorName + " đã gửi khóa học \"" + safeCourseTitle + "\" để chờ duyệt.",
                "Khóa học chờ duyệt - " + safeCourseTitle,
                paragraphs(safeInstructorName + " đã gửi khóa học \"" + safeCourseTitle + "\" để chờ duyệt."),
                courseId,
                "/admin/courses/" + courseId,
                metadata("courseId", courseId, "courseTitle", safeCourseTitle, "instructorName", safeInstructorName)
        );
    }

    public NotificationMessage certificateRequested(String requestId, String studentName, String courseId, String courseTitle) {
        String safeCourseTitle = defaultText(courseTitle, "khóa học của bạn");
        String safeStudentName = defaultText(studentName, "Một học viên");
        return message(
                "CERTIFICATE_REQUESTED",
                "Yêu cầu cấp chứng chỉ mới",
                safeStudentName + " đã yêu cầu cấp chứng chỉ cho khóa học \"" + safeCourseTitle + "\".",
                "Yêu cầu cấp chứng chỉ - " + safeCourseTitle,
                paragraphs(
                        safeStudentName + " đã yêu cầu cấp chứng chỉ cho khóa học \"" + safeCourseTitle + "\".",
                        "Vui lòng kiểm tra và xử lý yêu cầu trong trang quản lý."
                ),
                requestId,
                "/instructor/certificates/requests/" + requestId,
                metadata("requestId", requestId, "courseId", courseId, "courseTitle", safeCourseTitle)
        );
    }

    public NotificationMessage certificateApproved(String requestId, String userName, String courseId,
                                                   String courseTitle, String certificateUrl) {
        String safeCourseTitle = defaultText(courseTitle, "khóa học của bạn");
        String greeting = userName == null || userName.isBlank() ? "Chúc mừng!" : "Chúc mừng " + userName + "!";
        return message(
                "CERTIFICATE_APPROVED",
                "Chứng chỉ đã được duyệt",
                "Chứng chỉ của bạn cho khóa học \"" + safeCourseTitle + "\" đã được duyệt.",
                "Chứng chỉ đã được duyệt - " + safeCourseTitle,
                paragraphs(
                        greeting,
                        "Chứng chỉ của bạn cho khóa học \"" + safeCourseTitle + "\" đã được duyệt.",
                        certificateUrl == null || certificateUrl.isBlank()
                                ? "Bạn có thể xem chứng chỉ trong hồ sơ học tập."
                                : "<a href=\"" + escape(certificateUrl) + "\">Xem chứng chỉ</a>"
                ),
                requestId,
                certificateUrl,
                metadata("requestId", requestId, "courseId", courseId, "courseTitle", safeCourseTitle)
        );
    }

    public NotificationMessage courseCompleted(String courseId, String courseTitle) {
        String safeCourseTitle = defaultText(courseTitle, "khóa học");
        return message(
                "COURSE_COMPLETED",
                "Bạn đã hoàn thành khóa học",
                "Chúc mừng bạn đã hoàn thành khóa học \"" + safeCourseTitle + "\".",
                "Hoàn thành khóa học - " + safeCourseTitle,
                paragraphs(
                        "Chúc mừng bạn đã hoàn thành khóa học \"" + safeCourseTitle + "\".",
                        "Hãy tiếp tục duy trì nhịp học và khám phá các khóa học tiếp theo."
                ),
                courseId,
                courseAction(courseId),
                metadata("courseId", courseId, "courseTitle", safeCourseTitle)
        );
    }

    public NotificationMessage dailyReminder(String goalType, int targetValue, int currentValue, String targetItemId) {
        String message = switch (goalType == null ? "" : goalType) {
            case "XP" -> "Mục tiêu hôm nay của bạn là " + targetValue + " XP. Tiếp tục học nhé.";
            case "LEARNING_ITEMS_COMPLETED" -> "Bạn cần hoàn thành " + targetValue + " nội dung học trong hôm nay.";
            case "VIDEOS_COMPLETED" -> "Bạn cần hoàn thành " + targetValue + " bài học video trong hôm nay.";
            case "QUIZZES_PASSED" -> "Bạn cần vượt qua " + targetValue + " bài quiz trong hôm nay.";
            case "ASSIGNMENTS_SUBMITTED" -> "Bạn cần nộp " + targetValue + " bài tập trong hôm nay.";
            case "SPECIFIC_LESSON_COMPLETED" -> "Bạn có một bài học cần hoàn thành trong hôm nay.";
            default -> "Bạn có một mục tiêu học tập cần hoàn thành trong hôm nay.";
        };
        return message(
                "DAILY_LEARNING_REMINDER",
                "Đừng để mất nhịp học",
                message,
                "Nhắc học hôm nay",
                paragraphs(message, "Mở CINX để tiếp tục tiến độ học tập của bạn."),
                targetItemId,
                targetItemId == null ? "/learning" : "/learning/items/" + targetItemId,
                metadata("goalType", goalType, "targetValue", targetValue, "currentValue", currentValue, "targetItemId", targetItemId)
        );
    }

    /** A study reminder learning already chose and wrote; it is only delivered here. */
    public NotificationMessage studyReminder(String reason, String title, String body, String actionUrl, String writtenBy) {
        return message("STUDY_REMINDER", title, body, title, paragraphs(body), null, actionUrl,
                metadata("reason", reason, "writtenBy", writtenBy));
    }

    /** An AI subtitle job ended: the track waits for the instructor's review, or the job failed. */
    public NotificationMessage subtitleJobFinished(String jobId, String outcome, String displayName, String lessonTitle,
                                                   int warningCount, String actionUrl) {
        String track = defaultText(displayName, "phụ đề");
        String lesson = defaultText(lessonTitle, "bài học");
        boolean failed = "FAILED".equals(outcome);
        String title = failed ? "Tạo phụ đề không thành công" : "Phụ đề cần duyệt";
        String body = failed
                ? "Không tạo được phụ đề «" + track + "» cho bài «" + lesson + "». Bạn có thể thử lại trong trình sửa bài."
                : "Phụ đề «" + track + "» của bài «" + lesson + "» đã xong"
                        + (warningCount > 0 ? ", có " + warningCount + " cảnh báo chất lượng" : "")
                        + ". Học viên chỉ thấy sau khi bạn duyệt.";
        return message(failed ? "SUBTITLE_FAILED" : "SUBTITLE_REVIEW", title, body, title, paragraphs(body), jobId, actionUrl,
                metadata("outcome", outcome, "warningCount", warningCount));
    }

    public NotificationMessage paymentSucceeded(String orderId, String formattedPrice, String userName) {
        String price = defaultText(formattedPrice, "0 VND");
        return message(
                "PAYMENT_SUCCEEDED",
                "Thanh toán thành công",
                "Thanh toán cho đơn hàng " + orderId + " (" + price + ") đã được xử lý thành công.",
                "Xác nhận thanh toán - Đơn hàng " + orderId,
                paragraphs(
                        "Xin chào " + defaultText(userName, "bạn") + ",",
                        "CINX đã nhận thanh toán cho đơn hàng \"" + orderId + "\".",
                        "Tổng tiền: " + price + ". Cảm ơn bạn đã mua khóa học."
                ),
                orderId,
                "/orders/" + orderId,
                metadata("orderId", orderId, "totalPrice", price)
        );
    }

    public NotificationMessage orderCreated(String orderId) {
        return message(
                "ORDER_CREATED",
                "Đơn hàng đã được tạo",
                "Đơn hàng " + orderId + " đã được tạo thành công.",
                "Đơn hàng đã được tạo - " + orderId,
                paragraphs("Đơn hàng " + orderId + " đã được tạo thành công."),
                orderId,
                "/orders/" + orderId,
                metadata("orderId", orderId)
        );
    }

    public NotificationMessage orderCancelled(String orderId, String formattedPrice, String userName) {
        String price = defaultText(formattedPrice, "0 VND");
        return message(
                "ORDER_CANCELLED",
                "Đơn hàng đã bị hủy",
                "Đơn hàng " + orderId + " (" + price + ") đã bị hủy.",
                "Đơn hàng đã bị hủy - " + orderId,
                paragraphs(
                        "Xin chào " + defaultText(userName, "bạn") + ",",
                        "Đơn hàng \"" + orderId + "\" đã bị hủy.",
                        "Tổng tiền: " + price + "."
                ),
                orderId,
                "/orders/" + orderId,
                metadata("orderId", orderId, "totalPrice", price)
        );
    }

    public NotificationMessage courseReviewCreated(String courseId, String courseTitle, String reviewerName, Double rating) {
        String safeCourseTitle = defaultText(courseTitle, "khóa học của bạn");
        String ratingText = rating == null ? "một đánh giá mới" : String.format("%.1f sao", rating);
        return message(
                "COURSE_REVIEW_CREATED",
                "Đánh giá mới cho khóa học",
                defaultText(reviewerName, "Một học viên") + " đã để lại đánh giá " + ratingText
                        + " cho khóa học \"" + safeCourseTitle + "\".",
                "Đánh giá mới - " + safeCourseTitle,
                paragraphs(defaultText(reviewerName, "Một học viên") + " đã để lại đánh giá " + ratingText
                        + " cho khóa học \"" + safeCourseTitle + "\"."),
                courseId,
                courseAction(courseId),
                metadata("courseId", courseId, "courseTitle", safeCourseTitle, "rating", rating)
        );
    }

    public NotificationMessage courseQuestionCreated(String courseId, String courseTitle, String questionId, String questionTitle) {
        String safeCourseTitle = defaultText(courseTitle, "khóa học của bạn");
        String safeQuestionTitle = defaultText(questionTitle, "một câu hỏi mới");
        return message(
                "COURSE_QUESTION_CREATED",
                "Câu hỏi mới trong khóa học",
                "Có câu hỏi mới trong \"" + safeCourseTitle + "\": " + safeQuestionTitle,
                "Câu hỏi mới - " + safeCourseTitle,
                paragraphs("Có câu hỏi mới trong \"" + safeCourseTitle + "\": " + safeQuestionTitle),
                questionId,
                courseAction(courseId) + "/questions/" + questionId,
                metadata("courseId", courseId, "courseTitle", safeCourseTitle, "questionId", questionId)
        );
    }

    public NotificationMessage courseAnswerCreated(String courseId, String questionId, String answerId, boolean replyToAnswer) {
        return message(
                "COURSE_ANSWER_CREATED",
                "Phản hồi mới trong Q&A",
                replyToAnswer
                        ? "Có người đã phản hồi câu trả lời của bạn trong Q&A khóa học."
                        : "Có người đã trả lời câu hỏi của bạn trong Q&A khóa học.",
                "Phản hồi mới trong Q&A",
                paragraphs(replyToAnswer
                        ? "Có người đã phản hồi câu trả lời của bạn trong Q&A khóa học."
                        : "Có người đã trả lời câu hỏi của bạn trong Q&A khóa học."),
                answerId,
                courseAction(courseId) + "/questions/" + questionId,
                metadata("courseId", courseId, "questionId", questionId, "answerId", answerId)
        );
    }

    /** roleLabel is the Vietnamese role ("Quản lý" / "Biên tập"); the invitee answers in their Khoá học › Lời mời. */
    public NotificationMessage courseCollaboratorInvited(String courseId, String courseTitle, String inviterName, String roleLabel) {
        String safeCourseTitle = defaultText(courseTitle, "một khóa học");
        String safeInviterName = defaultText(inviterName, "Một giảng viên");
        String safeRole = defaultText(roleLabel, "cùng dạy");
        return message(
                "COURSE_COLLABORATOR_INVITED",
                "Lời mời cùng dạy",
                safeInviterName + " mời bạn làm " + safeRole + " khóa học \"" + safeCourseTitle + "\".",
                "Lời mời cùng dạy - " + safeCourseTitle,
                paragraphs(
                        safeInviterName + " mời bạn làm " + safeRole + " khóa học \"" + safeCourseTitle + "\".",
                        "Mở Kênh giảng viên › Khoá học › Lời mời để nhận lời hoặc từ chối."
                ),
                courseId,
                "/instructor/courses?tab=invitations",
                metadata("courseId", courseId, "courseTitle", safeCourseTitle, "inviterName", safeInviterName, "role", safeRole)
        );
    }

    /** An instructor scheduled a campus event in their zone; their learners hear about it. */
    public NotificationMessage campusEventScheduled(String eventId, String zoneId, String zoneTitle, String eventTitle,
                                                    String kind, Instant startsAt) {
        String safeTitle = defaultText(eventTitle, "một sự kiện");
        String zone = defaultText(zoneTitle, "Một khu trên Campus");
        String title = "Sự kiện mới trên Campus";
        String body = zone + " vừa lên lịch " + campusKind(kind) + " \"" + safeTitle + "\"" + campusTime(startsAt)
                + ". Bật nhắc để không bỏ lỡ nhé.";
        return message("CAMPUS_EVENT_SCHEDULED", title, body, title + " - " + safeTitle, paragraphs(body), eventId,
                campusAction(zoneId), campusMetadata(eventId, zoneId, zone, kind, startsAt));
    }

    /** A campus event the learner set a reminder for starts within 15 minutes. */
    public NotificationMessage campusEventReminder(String eventId, String zoneId, String zoneTitle, String eventTitle,
                                                   String kind, Instant startsAt) {
        String safeTitle = defaultText(eventTitle, "Sự kiện bạn hẹn");
        String zone = defaultText(zoneTitle, "Campus");
        String title = "Sự kiện sắp bắt đầu";
        String body = capitalize(campusKind(kind)) + " \"" + safeTitle + "\" ở " + zone + " bắt đầu" + campusTime(startsAt)
                + ". Vào khu thôi!";
        return message("CAMPUS_EVENT_REMINDER", title, body, title + " - " + safeTitle, paragraphs(body), eventId,
                campusAction(zoneId), campusMetadata(eventId, zoneId, zone, kind, startsAt));
    }

    /** A campus event the learner set a reminder for was cancelled by its host. */
    public NotificationMessage campusEventCancelled(String eventId, String zoneId, String zoneTitle, String eventTitle,
                                                    Instant startsAt) {
        String safeTitle = defaultText(eventTitle, "Sự kiện bạn hẹn");
        String zone = defaultText(zoneTitle, "Campus");
        String title = "Sự kiện đã bị hủy";
        String body = "Sự kiện \"" + safeTitle + "\" ở " + zone + campusTime(startsAt) + " đã bị hủy.";
        return message("CAMPUS_EVENT_CANCELLED", title, body, title + " - " + safeTitle, paragraphs(body), eventId,
                campusAction(zoneId), campusMetadata(eventId, zoneId, zone, null, startsAt));
    }

    /** Someone replied to the learner's thread on a zone's or club's board; the link opens the board on that thread. */
    public NotificationMessage campusThreadReply(String threadId, String replyId, String placeKind, String placeId,
                                                 String placeTitle, String threadTitle, String replierName) {
        String safeTitle = defaultText(threadTitle, "Bài viết của bạn");
        String name = defaultText(replierName, "Một bạn học");
        String title = "Có trả lời mới trên Campus";
        String post = threadTitle == null || threadTitle.isBlank() ? "bài viết" : "bài \"" + threadTitle + "\"";
        String where = placeTitle == null || placeTitle.isBlank() ? "trên bảng tin Campus" : "ở " + placeTitle;
        String body = name + " vừa trả lời " + post + " của bạn " + where + ".";
        String action = placeId == null || placeId.isBlank() ? "/campus" : "/campus?zone=" + URLEncoder.encode(placeId, StandardCharsets.UTF_8)
                + (threadId == null || threadId.isBlank() ? "" : "&thread=" + URLEncoder.encode(threadId, StandardCharsets.UTF_8));
        return message("CAMPUS_THREAD_REPLY", title, body, title + " - " + safeTitle, paragraphs(body), threadId, action,
                metadata("threadId", threadId, "replyId", replyId, "placeKind", placeKind, "placeId", placeId, "placeTitle", placeTitle));
    }

    /** Someone asked the learner to be friends; the request waits in the profile's "Bạn bè" tab. */
    public NotificationMessage friendRequested(String requestId, String fromUserId, String fromName) {
        String name = defaultText(fromName, "Một bạn học");
        String title = "Lời mời kết bạn";
        String body = name + " muốn kết bạn với bạn. Mở Bạn bè để chấp nhận hoặc từ chối.";
        return message("FRIEND_REQUEST", title, body, title + " - " + name, paragraphs(body), requestId,
                FRIENDS_ACTION, metadata("requestId", requestId, "fromUserId", fromUserId, "fromName", name));
    }

    /** The person the learner asked said yes; they are friends now. */
    public NotificationMessage friendAccepted(String requestId, String accepterId, String accepterName) {
        String name = defaultText(accepterName, "Một bạn học");
        String title = "Hai bạn đã là bạn bè";
        String body = name + " đã chấp nhận lời mời kết bạn của bạn. Hẹn gặp nhau ở Campus nhé.";
        return message("FRIEND_ACCEPTED", title, body, title + " - " + name, paragraphs(body), requestId,
                FRIENDS_ACTION, metadata("requestId", requestId, "accepterId", accepterId, "accepterName", name));
    }

    public NotificationMessage labSessionReady(String sessionId, String courseId, String lessonId, String lessonTitle) {
        String lesson = defaultText(lessonTitle, "bài lab");
        String title = "Máy lab đã sẵn sàng";
        String body = "Đến lượt bạn: máy cho \"" + lesson + "\" đã bật. Vào làm ngay, thời gian của phiên đã bắt đầu tính.";
        return message("LAB_SESSION_READY", title, body, title + " - " + lesson, paragraphs(body), sessionId, lessonAction(courseId, lessonId),
                metadata("sessionId", sessionId, "courseId", courseId, "lessonId", lessonId));
    }

    public NotificationMessage labSessionEnding(String sessionId, String courseId, String lessonId, String lessonTitle, int minutesLeft) {
        String lesson = defaultText(lessonTitle, "bài lab");
        String title = "Máy lab còn " + minutesLeft + " phút";
        String body = minutesLeft <= 2
                ? "Máy của \"" + lesson + "\" sắp hết giờ. Nộp bài ngay để không mất lần làm này."
                : "Máy của \"" + lesson + "\" còn " + minutesLeft + " phút. Hết giờ, máy và mọi tệp trong máy bị xoá: nộp bài hoặc tải thư mục về trước.";
        return message("LAB_SESSION_ENDING", title, body, title + " - " + lesson, paragraphs(body), sessionId, lessonAction(courseId, lessonId),
                metadata("sessionId", sessionId, "courseId", courseId, "lessonId", lessonId, "minutesLeft", minutesLeft));
    }

    public NotificationMessage labReviewDecided(String releaseId, String courseId, String lessonId, String lessonTitle, boolean approved, String reason) {
        String lesson = defaultText(lessonTitle, "bài lab");
        String title = approved ? "Bài lab đã được duyệt" : "Bài lab chưa được duyệt";
        String body = approved
                ? "Bản phát hành của \"" + lesson + "\" đã được duyệt; học viên giờ dùng bản này."
                : "Bản phát hành của \"" + lesson + "\" bị từ chối: " + defaultText(reason, "không có lý do") + " Học viên vẫn dùng bản trước.";
        return message(approved ? "LAB_REVIEW_APPROVED" : "LAB_REVIEW_REJECTED", title, body, title + " - " + lesson, paragraphs(body), releaseId,
                studioLessonAction(courseId, lessonId), metadata("releaseId", releaseId, "courseId", courseId, "lessonId", lessonId));
    }

    private String lessonAction(String courseId, String lessonId) {
        if (courseId == null || courseId.isBlank()) {
            return "/learning";
        }
        return "/learning/" + courseId + (lessonId == null || lessonId.isBlank() ? "" : "?lessonId=" + URLEncoder.encode(lessonId, StandardCharsets.UTF_8));
    }

    private String studioLessonAction(String courseId, String lessonId) {
        if (courseId == null || courseId.isBlank()) {
            return "/instructor/courses";
        }
        return "/instructor/courses/" + courseId + "/edit" + (lessonId == null || lessonId.isBlank() ? "" : "?lesson=" + URLEncoder.encode(lessonId, StandardCharsets.UTF_8));
    }

    private NotificationMessage message(String type, String title, String message, String subject, String htmlBody,
                                        String referenceId, String actionUrl, Map<String, Object> metadata) {
        return new NotificationMessage(type, title, message, subject, htmlBody, referenceId, actionUrl, metadata);
    }

    private String paragraphs(String... lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (line.startsWith("<a ")) {
                builder.append("<p>").append(line).append("</p>");
            } else {
                builder.append("<p>").append(escape(line)).append("</p>");
            }
        }
        return builder.toString();
    }

    private Map<String, Object> metadata(Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i] != null && keyValues[i + 1] != null) {
                metadata.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
            }
        }
        return metadata;
    }

    private String courseAction(String courseId) {
        return courseId == null || courseId.isBlank() ? "/courses" : "/courses/" + courseId;
    }

    private String campusAction(String zoneId) {
        return zoneId == null || zoneId.isBlank() ? "/campus" : "/campus?zone=" + URLEncoder.encode(zoneId, StandardCharsets.UTF_8);
    }

    private Map<String, Object> campusMetadata(String eventId, String zoneId, String zoneTitle, String kind, Instant startsAt) {
        return metadata("eventId", eventId, "zoneId", zoneId, "zoneTitle", zoneTitle, "kind", kind,
                "startsAt", startsAt == null ? null : startsAt.toString());
    }

    private String campusKind(String kind) {
        return switch (kind == null ? "" : kind) {
            case "quiz" -> "quiz";
            case "lecture" -> "buổi giảng";
            case "meetup" -> "buổi giao lưu";
            default -> "sự kiện";
        };
    }

    /** " lúc 20:00 thứ Sáu 14/11" in Vietnam time, or nothing when the time is unknown. */
    private String campusTime(Instant startsAt) {
        if (startsAt == null) {
            return "";
        }
        ZonedDateTime local = startsAt.atZone(CAMPUS_TIME_ZONE);
        String weekday = switch (local.getDayOfWeek()) {
            case MONDAY -> "thứ Hai";
            case TUESDAY -> "thứ Ba";
            case WEDNESDAY -> "thứ Tư";
            case THURSDAY -> "thứ Năm";
            case FRIDAY -> "thứ Sáu";
            case SATURDAY -> "thứ Bảy";
            case SUNDAY -> "Chủ nhật";
        };
        return " lúc " + CLOCK.format(local) + " " + weekday + " " + DAY_MONTH.format(local);
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
