package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ITextService {

    public String extractTextFromPdf(String pdfPath) {
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new ApiException("PDF path is required");
        }
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new ApiException("Failed to extract PDF text: " + e.getMessage());
        }
    }

    public byte[] generateMonthlyReportPdf(String reportContent) {
        if (reportContent == null || reportContent.isBlank()) {
            throw new ApiException("Report content is required");
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText(reportContent);
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ApiException("Failed to generate PDF: " + e.getMessage());
        }
    }
}
