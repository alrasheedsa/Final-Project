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

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }
}