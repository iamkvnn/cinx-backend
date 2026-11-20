package com.cinx.notification.dispatch.infrastructure.channel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EmailNotificationStrategyTest {

    @Mock
    private JavaMailSender mailSender;

    private String invokeWrapInHtmlTemplate(String subject, String body) throws Exception {
        EmailNotificationStrategy strategy = new EmailNotificationStrategy(mailSender);
        Method method = EmailNotificationStrategy.class.getDeclaredMethod("wrapInHtmlTemplate", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(strategy, subject, body);
    }

    @Test
    void testWrapInHtmlTemplate_PlainText() throws Exception {
        String body = "Hello John,\n\nWelcome to CINX!\nBest regards.";
        String result = invokeWrapInHtmlTemplate("Welcome", body);

        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("class='cinx-email email-container'"));
        assertTrue(result.contains("<h1>Welcome</h1>"));
        assertTrue(result.contains("Hello John,<br/><br/>Welcome to CINX!<br/>Best regards."));
        assertTrue(result.contains("Mọi quyền được bảo lưu."));
    }

    @Test
    void testWrapInHtmlTemplate_SimpleHtmlWithBody() throws Exception {
        String body = "<html><body><h2>Payment Successful</h2><p>Dear Jane,</p><p>Total: <b>100k</b></p></body></html>";
        String result = invokeWrapInHtmlTemplate("Payment Confirmation", body);

        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<h1>Payment Confirmation</h1>"));
        assertTrue(result.contains("<h2>Payment Successful</h2>"));
        assertTrue(result.contains("<p>Dear Jane,</p>"));
        assertTrue(result.contains("<p>Total: <b>100k</b></p>"));
        assertFalse(result.contains("<html><body><h2>Payment Successful</h2>"));
        assertFalse(result.contains("<b>100k</b></p></body></html>"));
    }

    @Test
    void testWrapInHtmlTemplate_SimpleHtmlWithoutBodyTag() throws Exception {
        String body = "<html><h3>Course Update</h3><p>New lesson added.</p></html>";
        String result = invokeWrapInHtmlTemplate("Course Update", body);

        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<h3>Course Update</h3>"));
        assertTrue(result.contains("<p>New lesson added.</p>"));
        assertFalse(result.contains("<html><h3>"));
    }

    @Test
    void testWrapInHtmlTemplate_AlreadyWrapped() throws Exception {
        String body = "<div class=\"cinx-email\">Already formatted professional email</div>";
        String result = invokeWrapInHtmlTemplate("Notification", body);

        assertTrue(result.equals(body));
    }

    @Test
    void testWrapInHtmlTemplate_EscapesSubject() throws Exception {
        String result = invokeWrapInHtmlTemplate("<b>Unsafe</b>", "Hello");

        assertTrue(result.contains("<h1>&lt;b&gt;Unsafe&lt;/b&gt;</h1>"));
        assertFalse(result.contains("<h1><b>Unsafe</b></h1>"));
    }
}
