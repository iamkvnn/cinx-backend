package com.cinx.learning.certificate.application;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CertificateGeneratorService {

    public byte[] generateCertificate(String studentName, String courseTitle, String certificateTitle) {
        try {
            // 1. Tạo Canvas tĩnh kích thước 1200x800
            int width = 1200;
            int height = 800;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Bật khử răng cưa để chữ và hình vẽ mượt mà
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 2. Vẽ Background màu trắng/kem nhạt
            g.setColor(new Color(252, 252, 250));
            g.fillRect(0, 0, width, height);

            // 3. Vẽ Khung viền (Border)
            g.setColor(new Color(30, 60, 115)); // Xanh đen đậm
            g.setStroke(new BasicStroke(12));
            g.drawRect(40, 40, width - 80, height - 80);

            g.setColor(new Color(200, 160, 50)); // Màu vàng Gold
            g.setStroke(new BasicStroke(4));
            g.drawRect(55, 55, width - 110, height - 110);

            // 4. Định nghĩa các Font chữ
            Font providerFont = new Font("Serif", Font.BOLD, 28);
            Font titleFont = new Font("Serif", Font.BOLD, 55);
            Font normalFont = new Font("Serif", Font.PLAIN, 25);
            Font nameFont = new Font("Serif", Font.BOLD | Font.ITALIC, 75);
            Font courseFont = new Font("Serif", Font.BOLD, 40);
            Font dateFont = new Font("Serif", Font.PLAIN, 22);

            // 5. Thêm Tên Hệ thống (Cinx E-Learning)
            g.setFont(providerFont);
            g.setColor(new Color(30, 60, 115));
            String providerText = "CINX E-LEARNING SYSTEM";
            FontMetrics metrics = g.getFontMetrics(providerFont);
            g.drawString(providerText, (width - metrics.stringWidth(providerText)) / 2, 120);

            // 6. Vẽ Tiêu đề Chứng chỉ
            g.setFont(titleFont);
            g.setColor(Color.BLACK);
            metrics = g.getFontMetrics(titleFont);
            g.drawString(certificateTitle, (width - metrics.stringWidth(certificateTitle)) / 2, 230);

            // Text phụ
            g.setFont(normalFont);
            String subText1 = "This is to proudly certify that";
            metrics = g.getFontMetrics(normalFont);
            g.drawString(subText1, (width - metrics.stringWidth(subText1)) / 2, 310);

            // 7. Vẽ Tên Học viên
            g.setFont(nameFont);
            g.setColor(new Color(200, 50, 50)); // Đỏ mận
            metrics = g.getFontMetrics(nameFont);
            g.drawString(studentName, (width - metrics.stringWidth(studentName)) / 2, 420);

            // Text phụ
            g.setFont(normalFont);
            g.setColor(Color.BLACK);
            String subText2 = "has successfully completed the course";
            metrics = g.getFontMetrics(normalFont);
            g.drawString(subText2, (width - metrics.stringWidth(subText2)) / 2, 500);

            // 8. Vẽ Tên Khóa học
            g.setFont(courseFont);
            g.setColor(Color.DARK_GRAY);
            metrics = g.getFontMetrics(courseFont);
            g.drawString(courseTitle, (width - metrics.stringWidth(courseTitle)) / 2, 580);

            // 9. Vẽ Ngày cấp và Chữ ký
            g.setFont(dateFont);
            g.setColor(Color.BLACK);
            String dateText = "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            g.drawString(dateText, 150, 700);

            // Vẽ đường gạch dưới cho chữ ký
            g.setStroke(new BasicStroke(2));
            g.drawLine(width - 350, 670, width - 150, 670);
            g.drawString("System Administrator", width - 335, 700);

            g.dispose();

            // 10. Xuất ra byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating certificate image", e);
        }
    }
}