package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    public String sendEmail(String to, String subject, String body) {
        validateText(to, "Recipient email is required");
        validateText(subject, "Email subject is required");
        validateText(body, "Email body is required");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        logger.info("Email effectively dispatched to: " + to);

        return "Email has been sent";
    }

    public String sendEmailWithPdf(String to, String subject, String body, byte[] pdf) {
        validateText(to, "Recipient email is required");
        validateText(subject, "Email subject is required");
        validateText(body, "Email body is required");

        if (pdf == null || pdf.length == 0) {
            throw new ApiException("PDF attachment is required");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment("monthly-report.pdf", new ByteArrayResource(pdf));

            mailSender.send(message);
            logger.info("Email with PDF effectively dispatched to: " + to);
        } catch (Exception e) {
            throw new ApiException("Failed to send email with PDF: " + e.getMessage());
        }

        return "Email with PDF has been sent";
    }

    public String sendHtmlEmail(String to, String subject, String htmlBody) {
        validateText(to, "Recipient email is required");
        validateText(subject, "Email subject is required");
        validateText(htmlBody, "Email body is required");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            logger.info("HTML email effectively dispatched to: " + to);

            return "HTML email has been sent";
        } catch (Exception e) {
            throw new ApiException("Failed to send HTML email: " + e.getMessage());
        }
    }

    public String sendAIAnalysisReadyEmail(String to,
                                           String ownerName,
                                           String branchName,
                                           Integer month,
                                           Integer year,
                                           String slowHours,
                                           String peakHours,
                                           String topProducts,
                                           String lowProducts,
                                           String recommendation) {

        String subject = "تم تحليل سجل المبيعات - على دربك";

        String htmlBody = """
            <div style="margin:0;padding:0;background:#F7F3EA;font-family:Arial,Tahoma,sans-serif;direction:rtl;text-align:right;color:#243B35;">
                <div style="max-width:680px;margin:0 auto;padding:32px 18px;">
                    
                    <div style="background:#ffffff;border-radius:24px;padding:28px;border:1px solid #E6DDCC;box-shadow:0 10px 30px rgba(36,59,53,0.08);">
                        
                        <div style="margin-bottom:22px;">
                            <div style="font-size:24px;font-weight:800;color:#1F5C4D;">على دربك</div>
                            <div style="font-size:13px;color:#8B7D68;margin-top:6px;">تحليل ذكي لمبيعات المتجر</div>
                        </div>

                        <h2 style="margin:0 0 12px;font-size:22px;color:#243B35;">تم تحليل سجل المبيعات بنجاح</h2>
                        
                        <p style="font-size:15px;line-height:1.9;margin:0 0 20px;color:#5E6B63;">
                            مرحبًا %s، تم تحليل سجل مبيعات فرع <b>%s</b> لشهر <b>%s/%s</b>.
                            هذه لمحة سريعة عن أهم النتائج.
                        </p>

                        <div style="display:block;background:#F7F3EA;border-radius:18px;padding:18px;margin-bottom:18px;">
                            <div style="font-size:14px;color:#8B7D68;margin-bottom:6px;">وقت الركود المكتشف</div>
                            <div style="font-size:20px;font-weight:800;color:#1F5C4D;">%s</div>
                        </div>

                        <div style="display:block;background:#EEF6F1;border-radius:18px;padding:18px;margin-bottom:18px;">
                            <div style="font-size:14px;color:#6D7D73;margin-bottom:6px;">وقت الذروة</div>
                            <div style="font-size:18px;font-weight:700;color:#243B35;">%s</div>
                        </div>

                        <div style="margin:20px 0;">
                            <div style="background:#ffffff;border:1px solid #E6DDCC;border-radius:16px;padding:16px;margin-bottom:10px;">
                                <div style="font-size:13px;color:#8B7D68;">الأكثر مبيعًا</div>
                                <div style="font-size:16px;font-weight:700;color:#243B35;margin-top:4px;">%s</div>
                            </div>

                            <div style="background:#ffffff;border:1px solid #E6DDCC;border-radius:16px;padding:16px;">
                                <div style="font-size:13px;color:#8B7D68;">يحتاج دعم أو تصريف</div>
                                <div style="font-size:16px;font-weight:700;color:#243B35;margin-top:4px;">%s</div>
                            </div>
                        </div>

                        <div style="background:#1F5C4D;color:#ffffff;border-radius:18px;padding:18px;margin-top:18px;">
                            <div style="font-size:14px;opacity:0.9;margin-bottom:8px;">توصية الذكاء الاصطناعي</div>
                            <div style="font-size:16px;line-height:1.8;font-weight:600;">%s</div>
                        </div>

                        <p style="font-size:13px;line-height:1.8;color:#8B7D68;margin:22px 0 0;">
                            يمكنك فتح لوحة التحليل داخل النظام لمراجعة الرسم البياني والتوصيات الكاملة وتوليد اقتراحات الحملات.
                        </p>
                    </div>

                    <div style="text-align:center;color:#A09686;font-size:12px;margin-top:18px;">
                        هذه رسالة تلقائية من منصة على دربك
                    </div>
                </div>
            </div>
            """.formatted(
                safeEmailText(ownerName),
                safeEmailText(branchName),
                month,
                year,
                safeEmailText(slowHours),
                safeEmailText(peakHours),
                safeEmailText(topProducts),
                safeEmailText(lowProducts),
                safeEmailText(recommendation)
        );

        return sendHtmlEmail(to, subject, htmlBody);
    }

    private String safeEmailText(String value) {
        if (value == null || value.isBlank()) {
            return "غير متوفر";
        }

        return value;
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }
}