package com.cinx.user.profile.infrastructure.messaging;

import com.cinx.user.shared.infrastructure.messaging.OutboxEventPublisher;

import com.cinx.user.profile.application.UserEvents;
import com.cinx.user.profile.domain.LearnerProfile;
import com.cinx.user.profile.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer implements UserEvents {

    private static final String EXCHANGE = "user.events.exchange";

    private final OutboxEventPublisher outboxEventPublisher;

    /** Email sent when an instructor application is approved. */
    @Override
    public void sendInstructorVerifiedEmail(User user) {
        publish("user.instructor.verified", Map.of(
                "userId", user.getUserId(),
                "role", "INSTRUCTOR",
                "to", user.getEmail(),
                "subject", "Chúc mừng bạn đã trở thành giảng viên!",
                "body", "Xin chào " + user.getName() + ",\n\n" +
                        "Chúng tôi rất vui mừng thông báo rằng bạn đã được xác minh là giảng viên trên nền tảng của chúng tôi. " +
                        "Bạn có thể bắt đầu tạo khóa học và chia sẻ kiến thức của mình với cộng đồng học viên.\n\n" +
                        "Nếu bạn có bất kỳ câu hỏi nào, đừng ngần ngại liên hệ với chúng tôi.\n\n" +
                        "Chúc bạn thành công trong hành trình giảng dạy!\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Cinx"
        ));
    }

    @Override
    public void sendInstructorRejectedEmail(User user) {
        publish("user.instructor.rejected", Map.of(
                "userId", user.getUserId(),
                "to", user.getEmail(),
                "subject", "Thông báo về việc xác minh giảng viên",
                "body", "Xin chào " + user.getName() + ",\n\n" +
                        "Chúng tôi rất tiếc phải thông báo rằng yêu cầu xác minh giảng viên của bạn đã bị từ chối. " +
                        "Điều này có thể do một số lý do, chẳng hạn như thông tin không đầy đủ hoặc không phù hợp với tiêu chí của chúng tôi.\n\n" +
                        "Nếu bạn muốn biết thêm chi tiết hoặc cần hỗ trợ để cải thiện hồ sơ của mình, vui lòng liên hệ với chúng tôi.\n\n" +
                        "Chúng tôi đánh giá cao sự quan tâm của bạn và hy vọng sẽ có cơ hội hợp tác trong tương lai.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Cinx"
        ));
    }

    /** Email sent when an instructor partnership is terminated. */
    @Override
    public void sendPartnershipTerminatedEmail(User user) {
        publish("user.instructor.terminated", Map.of(
                "userId", user.getUserId(),
                "to", user.getEmail(),
                "subject", "Thông báo về việc chấm dứt hợp tác giảng dạy",
                "body", partnershipTerminatedBody(user)
        ));
    }

    private String partnershipTerminatedBody(User user) {
        return "Xin chào " + user.getName() + ",\n\n" +
                "Chúng tôi rất tiếc phải thông báo rằng CINX quyết định ngưng hợp tác giảng dạy với bạn kể từ thời điểm này. " +
               "Tài khoản giảng viên của bạn đã bị khóa và bạn sẽ không thể đăng nhập vào nền tảng được nữa.\n\n" +
                partnershipTerminationReason(user) +
                "Toàn bộ số dư hiện tại trong tài khoản giảng viên của bạn sẽ được CINX thanh toán đầy đủ trong vòng 7 ngày làm việc qua thông tin tài khoản đã đăng ký.\n\n" +
                "Nếu bạn có bất kỳ thắc mắc hoặc cần thêm thông tin, vui lòng phản hồi qua email này để được hỗ trợ.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ Cinx";
    }

    private String partnershipTerminationReason(User user) {
        if (user.getPartnershipTerminationReasonType() == null) {
            return "";
        }
        String reason = "Ly do: " + user.getPartnershipTerminationReasonType().name();
        if (user.getPartnershipTerminationReasonDetail() != null) {
            reason += "\nChi tiet: " + user.getPartnershipTerminationReasonDetail();
        }
        return reason + "\n\n";
    }

    /** In-app + push notification to admins when a new instructor registers. */
    public void sendNewInstructorNotification(User user, List<String> adminUserIds) {
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            log.warn("No admin users found; skipping new instructor notification for userId={}", user.getUserId());
            return;
        }

        String title = "Có một giảng viên mới cần được xác minh!";
        String message = "Xin chào, có một giảng viên mới đã đăng ký và cần được xác minh. " +
                "Vui lòng kiểm tra và xác minh hồ sơ của giảng viên này để họ có thể bắt đầu tạo khóa học.";

        publish("user.instructor.pending", Map.of(
                "userId", user.getUserId(),
                "userIds", adminUserIds,
                "title", title,
                "message", message,
                "instructorEmail", user.getEmail()
        ));
    }

    @Override
    public void publishInstructorApplicationSubmitted(User user) {
        publish("user.instructor.application.submitted", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "userId", user.getUserId(),
                "email", user.getEmail(),
                "cvUrl", user.getCvUrl() == null ? "" : user.getCvUrl(),
                "submittedAt", LocalDateTime.now().toString()
        ));
    }

    @Override
    public void publishPreferredCategoriesUpdated(String userId, List<String> categoryIds) {
        publish("user.preference.updated", Map.of(
                "payload", Map.of(
                        "userId", userId,
                        "categoryIds", categoryIds
                )
        ));
    }

    @Override
    public void publishLearningProfileUpdated(LearnerProfile profile) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("userId", profile.getUserId());
        payload.put("goal", profile.getGoal() == null ? null : profile.getGoal().name());
        payload.put("level", profile.getLevel() == null ? null : profile.getLevel().name());
        payload.put("minutesPerDay", profile.getMinutesPerDay());
        payload.put("contentLanguage", profile.getContentLanguage() == null ? null : profile.getContentLanguage().name());
        payload.put("categoryIds", profile.getCategoryIds());
        payload.put("personalizationConsent", profile.isPersonalizationConsent());
        payload.put("usageAnalyticsConsent", profile.isUsageAnalyticsConsent());
        payload.put("occurredAt", profile.getUpdatedAt().toString() + "Z");
        publish("user.learning-profile.updated", payload);
    }

    @Override
    public void publishProfileOnboarded(User user, String keycloakSubject) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("userId", user.getUserId());
        payload.put("keycloakSubject", keycloakSubject);
        payload.put("email", user.getEmail());
        publish("user.profile.onboarded", payload);
    }

    private void publish(String routingKey, Map<String, Object> payload) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "UserEvent",
                routingKey,
                routingKey,
                EXCHANGE,
                routingKey,
                payload
        );
        log.info("User event published -> routingKey={}", routingKey);
    }
}
