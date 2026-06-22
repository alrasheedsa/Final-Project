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
    private static final String PRIMARY      = "#1a5c5b";
    private static final String PRIMARY_LIGHT= "#e8f4f4";
    private static final String ACCENT       = "#f0e8d8";
    private static final String TEXT_DARK    = "#1a3a3a";
    private static final String TEXT_MUTED   = "#5a7a7a";
    private static final String BORDER       = "#d4e6e6";
    private static final String WHITE        = "#ffffff";

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

    public String buildBrandedEmailHtml(String subtitle,
                                        String title,
                                        String greeting,
                                        String mainContentHtml,
                                        String footerNote) {

        return """
                <div style="margin:0;padding:0;background:#F7F3EA;font-family:Arial,Tahoma,sans-serif;direction:rtl;text-align:right;color:#243B35;">
                    <div style="max-width:680px;margin:0 auto;padding:32px 18px;">

                        <div style="background:#ffffff;border-radius:24px;padding:28px;border:1px solid #E6DDCC;box-shadow:0 10px 30px rgba(36,59,53,0.08);">

                            <div style="margin-bottom:22px;">
                                <div style="font-size:24px;font-weight:800;color:#1F5C4D;">على دربك</div>
                                <div style="font-size:13px;color:#8B7D68;margin-top:6px;">%s</div>
                            </div>

                            <h2 style="margin:0 0 12px;font-size:22px;color:#243B35;">%s</h2>

                            <p style="font-size:15px;line-height:1.9;margin:0 0 20px;color:#5E6B63;">
                                %s
                            </p>

                            %s

                            <p style="font-size:13px;line-height:1.8;color:#8B7D68;margin:22px 0 0;">
                                %s
                            </p>
                        </div>

                        <div style="text-align:center;color:#A09686;font-size:12px;margin-top:18px;">
                            هذه رسالة تلقائية من منصة على دربك
                        </div>
                    </div>
                </div>
                """.formatted(
                safeEmailText(subtitle),
                safeEmailText(title),
                safeEmailText(greeting),
                mainContentHtml == null ? "" : mainContentHtml,
                safeEmailText(footerNote)
        );
    }

    public String buildEmailHighlightCard(String label, String value) {
        return """
                <div style="background:#1F5C4D;color:#ffffff;border-radius:18px;padding:18px;margin-bottom:16px;">
                    <div style="font-size:14px;opacity:0.9;margin-bottom:8px;">%s</div>
                    <div style="font-size:20px;font-weight:800;line-height:1.7;">%s</div>
                </div>
                """.formatted(
                safeEmailText(label),
                safeEmailText(value)
        );
    }

    public String buildEmailSoftCard(String label, String value) {
        return """
                <div style="background:#F7F3EA;border-radius:18px;padding:18px;margin-bottom:16px;">
                    <div style="font-size:14px;color:#8B7D68;margin-bottom:6px;">%s</div>
                    <div style="font-size:17px;font-weight:700;color:#243B35;line-height:1.8;">%s</div>
                </div>
                """.formatted(
                safeEmailText(label),
                safeEmailText(value)
        );
    }

    public String buildEmailSoftHighlightCard(String label, String value) {
        return """
                <div style="display:block;background:#F7F3EA;border-radius:18px;padding:18px;margin-bottom:18px;">
                    <div style="font-size:14px;color:#8B7D68;margin-bottom:6px;">%s</div>
                    <div style="font-size:20px;font-weight:800;color:#1F5C4D;line-height:1.8;">%s</div>
                </div>
                """.formatted(
                safeEmailText(label),
                safeEmailText(value)
        );
    }

    public String buildEmailGreenTintCard(String label, String value) {
        return """
                <div style="display:block;background:#EEF6F1;border-radius:18px;padding:18px;margin-bottom:18px;">
                    <div style="font-size:14px;color:#6D7D73;margin-bottom:6px;">%s</div>
                    <div style="font-size:18px;font-weight:700;color:#243B35;line-height:1.8;">%s</div>
                </div>
                """.formatted(
                safeEmailText(label),
                safeEmailText(value)
        );
    }

    public String buildEmailInfoCard(String label, String value) {
        return """
                <div style="background:#ffffff;border:1px solid #E6DDCC;border-radius:16px;padding:16px;margin-bottom:10px;">
                    <div style="font-size:13px;color:#8B7D68;">%s</div>
                    <div style="font-size:16px;font-weight:700;color:#243B35;margin-top:4px;line-height:1.8;">%s</div>
                </div>
                """.formatted(
                safeEmailText(label),
                safeEmailText(value)
        );
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

        String greeting = """
                مرحبًا %s، تم تحليل سجل مبيعات فرع <b>%s</b> لشهر <b>%s/%s</b>.
                هذه لمحة سريعة عن أهم النتائج.
                """.formatted(
                safeEmailText(ownerName),
                safeEmailText(branchName),
                safeEmailText(month == null ? null : month.toString()),
                safeEmailText(year == null ? null : year.toString())
        );

        String content = "";
        content += buildEmailSoftHighlightCard("وقت الركود المكتشف", slowHours);
        content += buildEmailGreenTintCard("وقت الذروة", peakHours);
        content += buildEmailInfoCard("الأكثر مبيعًا", topProducts);
        content += buildEmailInfoCard("يحتاج دعم أو تصريف", lowProducts);
        content += buildEmailHighlightCard("توصية الذكاء الاصطناعي", recommendation);

        String htmlBody = buildBrandedEmailHtml(
                "تحليل ذكي لمبيعات المتجر",
                "تم تحليل سجل المبيعات بنجاح",
                greeting,
                content,
                "يمكنك فتح لوحة التحليل داخل النظام لمراجعة الرسم البياني والتوصيات الكاملة وتوليد اقتراحات الحملات."
        );

        return sendHtmlEmail(to, subject, htmlBody);
    }

    public String sendCampaignSuggestionApprovedEmail(String to,
                                                      String ownerName,
                                                      String branchName,
                                                      String suggestionTitle,
                                                      String campaignType,
                                                      String offerText,
                                                      String productName,
                                                      String startDate,
                                                      String endDate,
                                                      String startTime,
                                                      String endTime,
                                                      Integer targetCustomersCount,
                                                      Double discountValue) {

        String subject = "تم اعتماد اقتراح الحملة - على دربك";

        String targetText = targetCustomersCount == null ? "غير متوفر" : targetCustomersCount.toString();
        String discountText = discountValue == null ? "غير متوفر" : discountValue + "%";

        String greeting = """
                مرحبًا %s، تم اعتماد اقتراح حملة لفرع <b>%s</b>.
                """.formatted(
                safeEmailText(ownerName),
                safeEmailText(branchName)
        );

        String campaignDateText = """
                من %s إلى %s<br>
                من الساعة %s إلى %s
                """.formatted(
                safeEmailText(startDate),
                safeEmailText(endDate),
                safeEmailText(startTime),
                safeEmailText(endTime)
        );

        String content = "";
        content += buildEmailHighlightCard("عنوان الحملة", suggestionTitle);
        content += buildEmailSoftCard("نص العرض", offerText);
        content += buildEmailInfoCard("نوع الحملة", campaignType);
        content += buildEmailInfoCard("المنتج المقترح", productName);
        content += buildEmailInfoCard("موعد الحملة", campaignDateText);
        content += buildEmailInfoCard("عدد العملاء المستهدفين", targetText);
        content += buildEmailInfoCard("قيمة الخصم", discountText);

        String htmlBody = buildBrandedEmailHtml(
                "اعتماد اقتراح حملة ذكية",
                "تم اعتماد اقتراح الحملة بنجاح",
                greeting,
                content,
                "يمكنك الآن متابعة تجهيز الحملة من لوحة الحملات داخل النظام."
        );

        return sendHtmlEmail(to, subject, htmlBody);
    }

    public void sendMonthlyReportEmail(String to,
                                       String storeName,
                                       String branchName,
                                       String monthLabel,
                                       Double totalSales,
                                       Integer totalQuantity,
                                       String topProducts,
                                       byte[] pdfBytes) {
        validateText(to,         "Recipient email is required");
        validateText(storeName,  "Store name is required");
        validateText(branchName, "Branch name is required");
        validateText(monthLabel, "Month label is required");

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ApiException("PDF attachment is required");
        }

        String subject = "التقرير الشهري — " + storeName + " | " + branchName + " | " + monthLabel;

        String htmlBody = """
            <div style="margin:0;padding:0;background:#F7F3EA;font-family:Arial,Tahoma,sans-serif;direction:rtl;text-align:right;color:#243B35;">
                <div style="max-width:680px;margin:0 auto;padding:32px 18px;">
                    <div style="background:#ffffff;border-radius:24px;padding:28px;border:1px solid #E6DDCC;box-shadow:0 10px 30px rgba(36,59,53,0.08);">

                        <div style="margin-bottom:22px;">
                            <div style="font-size:24px;font-weight:800;color:#1F5C4D;">على دربك</div>
                            <div style="font-size:13px;color:#8B7D68;margin-top:6px;">التقرير الشهري للمبيعات</div>
                        </div>

                        <h2 style="margin:0 0 12px;font-size:22px;color:#243B35;">تقرير %s</h2>

                        <p style="font-size:15px;line-height:1.9;margin:0 0 20px;color:#5E6B63;">
                            السلام عليكم، يسعدنا مشاركتك ملخص أداء مبيعات
                            <b>%s</b> — فرع <b>%s</b> لشهر <b>%s</b>.
                            <br/>التقرير الكامل مرفق بهذا الإيميل كملف PDF.
                        </p>

                        <div style="margin-bottom:18px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td width="48%%" style="background:#F7F3EA;border-radius:18px;padding:18px;text-align:center;">
                                        <div style="font-size:22px;font-weight:800;color:#1F5C4D;">%.2f</div>
                                        <div style="font-size:12px;color:#8B7D68;margin-top:4px;">إجمالي المبيعات (ريال)</div>
                                    </td>
                                    <td width="4%%"></td>
                                    <td width="48%%" style="background:#EEF6F1;border-radius:18px;padding:18px;text-align:center;">
                                        <div style="font-size:22px;font-weight:800;color:#243B35;">%d</div>
                                        <div style="font-size:12px;color:#8B7D68;margin-top:4px;">إجمالي الوحدات المباعة</div>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <div style="background:#ffffff;border:1px solid #E6DDCC;border-radius:16px;padding:16px;margin-bottom:16px;">
                            <div style="font-size:13px;color:#8B7D68;">⭐ أعلى المنتجات مبيعاً</div>
                            <div style="font-size:15px;font-weight:700;color:#243B35;margin-top:6px;line-height:1.8;">%s</div>
                        </div>

                        <div style="background:#1F5C4D;color:#ffffff;border-radius:18px;padding:16px;text-align:center;">
                            <div style="font-size:14px;opacity:0.9;">
                                التحليل الكامل والتوصيات مرفقة في ملف PDF
                            </div>
                        </div>

                        <p style="font-size:13px;line-height:1.8;color:#8B7D68;margin:20px 0 0;">
                            يمكنك فتح الملف المرفق لمراجعة التحليل الذكي ومقارنة أداء الشهر الحالي بالشهر السابق.
                        </p>
                    </div>

                    <div style="text-align:center;color:#A09686;font-size:12px;margin-top:18px;">
                        هذه رسالة تلقائية من منصة على دربك
                    </div>
                </div>
            </div>
            """.formatted(
                safeEmailText(monthLabel),
                safeEmailText(storeName),
                safeEmailText(branchName),
                safeEmailText(monthLabel),
                totalSales,
                totalQuantity,
                safeEmailText(topProducts)
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment(
                    "تقرير-" + monthLabel + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf"
            );
            mailSender.send(message);
            logger.info("Monthly report email sent to: " + to);
        } catch (Exception e) {
            throw new ApiException("Failed to send monthly report email: " + e.getMessage());
        }
    }

    public void sendQrCodeEmail(String to,
                               String storeName,
                               String campaignTitle,
                               String qrCode,
                               byte[] qrPng) {
        validateText(to,            "Recipient email is required");
        validateText(storeName,     "Store name is required");
        validateText(campaignTitle, "Campaign title is required");
        validateText(qrCode,        "QR code is required");

        if (qrPng == null || qrPng.length == 0) {
            throw new ApiException("QR code image is required");
        }

        String subject = "رمز العرض الخاص بك — على دربك";

        String content = "";
        content += buildEmailHighlightCard("اسم المتجر", storeName);
        content += buildEmailSoftCard("الحملة", campaignTitle);
        content += buildEmailSoftHighlightCard("كود العرض (QR)", qrCode);

        String htmlBody = buildBrandedEmailHtml(
                "رمز العرض الخاص بك",
                "إليك رمز العرض الخاص بك",
                "أرفقنا لك صورة رمز QR. اعرضها للكاشير عند زيارة الفرع للاستفادة من العرض.",
                content,
                "صورة رمز QR مرفقة بهذا الإيميل."
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment("qr-" + qrCode + ".png", new ByteArrayResource(qrPng), "image/png");
            mailSender.send(message);
            logger.info("QR code email sent to: " + to);
        } catch (Exception e) {
            throw new ApiException("Failed to send QR code email: " + e.getMessage());
        }
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
