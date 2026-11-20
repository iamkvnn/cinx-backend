package com.cinx.notification.dispatch.infrastructure.channel;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationChannelStrategy {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(Map<String, Object> payload) {
        String to = (String) payload.get("to");
        String subject = (String) payload.get("subject");
        String body = (String) payload.get("body");

        if (to == null || subject == null || body == null) {
            log.error("Email payload missing required fields: {}", payload);
            throw new IllegalArgumentException("Email payload missing required fields: 'to', 'subject', 'body'");
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            
            String formattedBody = wrapInHtmlTemplate(subject, body);
            helper.setText(formattedBody, true); // true indicates HTML content

            javaMailSender.send(mimeMessage);
            log.info("Sent email to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String wrapInHtmlTemplate(String subject, String body) {
        if (body.contains("class=\"cinx-email\"") || body.contains("class='cinx-email'")) {
            return body;
        }

        String content = body;
        String lowerBody = body.toLowerCase();
        
        // Strip out basic outer html/body tags if present, to wrap them inside the template
        if (lowerBody.contains("<html")) {
            int bodyStart = lowerBody.indexOf("<body");
            int bodyEnd = lowerBody.indexOf("</body>");
            if (bodyStart != -1 && bodyEnd != -1) {
                int startClose = body.indexOf(">", bodyStart);
                if (startClose != -1 && startClose < bodyEnd) {
                    content = body.substring(startClose + 1, bodyEnd);
                }
            } else {
                content = body.replaceAll("(?i)</?html>", "").replaceAll("(?i)</?body>", "");
            }
        }
        
        // Convert plain text newlines to HTML line breaks only if no block HTML tags are present
        String htmlContent = content;
        if (!content.contains("<p>") && !content.contains("<h2>") && !content.contains("<h3>") 
                && !content.contains("<br") && !content.contains("<a ")) {
            htmlContent = content.replace("\n", "<br/>");
        }
        
        // Build a professional HTML frame
        String safeSubject = HtmlUtils.htmlEscape(subject);
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "  <meta charset='utf-8'>" +
               "  <style>" +
               "    body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; }" +
               "    .email-container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0; }" +
               "    .email-header { background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%); padding: 32px; text-align: center; color: #ffffff; }" +
               "    .email-header h1 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0; line-height: 1.3; }" +
               "    .email-header p { margin: 0 0 8px 0; font-size: 13px; opacity: 0.9; text-transform: uppercase; letter-spacing: 1px; font-weight: 600; }" +
               "    .email-body { padding: 40px 32px; color: #334155; font-size: 16px; line-height: 1.7; }" +
               "    .email-body p { margin-top: 0; margin-bottom: 16px; }" +
               "    .email-body h2 { margin-top: 0; margin-bottom: 16px; font-size: 20px; font-weight: 600; color: #1e293b; }" +
               "    .email-body h3 { margin-top: 0; margin-bottom: 16px; font-size: 18px; font-weight: 600; color: #1e293b; }" +
               "    .email-body a { color: #4f46e5; text-decoration: none; font-weight: 600; }" +
               "    .email-body a:hover { text-decoration: underline; }" +
               "    .email-footer { background-color: #f8fafc; padding: 24px 32px; border-top: 1px solid #e2e8f0; text-align: center; color: #64748b; font-size: 13px; }" +
               "    .email-footer a { color: #4f46e5; text-decoration: none; font-weight: 600; }" +
               "  </style>" +
               "</head>" +
               "<body>" +
               "  <div class='cinx-email email-container'>" +
               "    <div class='email-header'>" +
               "      <p>CINX Platform</p>" +
               "      <h1>" + safeSubject + "</h1>" +
               "    </div>" +
               "    <div class='email-body'>" +
               "      " + htmlContent + "" +
               "    </div>" +
               "    <div class='email-footer'>" +
               "      <p>© " + java.time.Year.now().getValue() + " CINX Learning Platform. Mọi quyền được bảo lưu.</p>" +
               "      <p>Email này được gửi tự động, vui lòng không trả lời trực tiếp. Ghé thăm chúng tôi tại <a href='https://shiny.id.vn'>shiny.id.vn</a>.</p>" +
               "    </div>" +
               "  </div>" +
               "</body>" +
               "</html>";
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }
}
