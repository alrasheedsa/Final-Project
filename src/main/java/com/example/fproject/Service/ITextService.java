package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ITextService {

    private static final String MONTHLY_REPORT_TEMPLATE = "templates/monthly-report.html";

    public byte[] generateMonthlyReportPdf(String storeName,
                                           String generatedAt,
                                           String campaignRows,
                                           String recommendations) {
        validateText(storeName, "Store name is required");
        validateText(generatedAt, "Report date is required");
        validateText(campaignRows, "Campaign rows are required");
        validateText(recommendations, "Recommendations are required");

        String html = loadMonthlyReportTemplate()
                .replace("{{storeName}}", escapeHtml(storeName))
                .replace("{{generatedAt}}", escapeHtml(generatedAt))
                .replace("{{campaignRows}}", campaignRows)
                .replace("{{recommendations}}", escapeHtml(recommendations));

        return renderHtmlToPdf(html);
    }

    public byte[] generateMonthlyReportPdf(String reportContent) {
        validateText(reportContent, "Report content is required");
        String row = buildCampaignRow("ملخص التقرير", "0", "0", "0%");
        return generateMonthlyReportPdf("على دربك", "غير محدد", row, reportContent);
    }

    public byte[] generateSalesMonthlyReportPdf(String storeName,
                                                String branchName,
                                                Integer month,
                                                Integer year,
                                                Double totalSales,
                                                Integer totalQuantity,
                                                String topProducts,
                                                String lowProducts,
                                                String peakHours,
                                                String slowHours,
                                                String summary) {
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        @page { size: A4; margin: 20mm; }
                        body { font-family: Arial, sans-serif; color: #222; line-height: 1.6; }
                        h1 { color: #6d28d9; }
                        table { width: 100%%; border-collapse: collapse; margin: 18px 0; }
                        td { padding: 10px; border: 1px solid #ddd; }
                        td:first-child { font-weight: bold; width: 35%%; background: #f7f4ff; }
                    </style>
                </head>
                <body>
                    <h1>Monthly Sales Report</h1>
                    <p><strong>Store:</strong> %s</p>
                    <p><strong>Branch:</strong> %s</p>
                    <p><strong>Period:</strong> %02d/%d</p>
                    <table>
                        <tr><td>Total sales</td><td>%.2f SAR</td></tr>
                        <tr><td>Total quantity</td><td>%d</td></tr>
                        <tr><td>Top products</td><td>%s</td></tr>
                        <tr><td>Low products</td><td>%s</td></tr>
                        <tr><td>Peak hour</td><td>%s</td></tr>
                        <tr><td>Slow hour</td><td>%s</td></tr>
                    </table>
                    <h2>Summary</h2>
                    <p>%s</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(storeName),
                escapeHtml(branchName),
                month,
                year,
                totalSales,
                totalQuantity,
                escapeHtml(topProducts),
                escapeHtml(lowProducts),
                escapeHtml(peakHours),
                escapeHtml(slowHours),
                escapeHtml(summary)
        );

        return renderHtmlToPdf(html);
    }

    public String buildCampaignRow(String campaignName,
                                   String totalSent,
                                   String qrUsed,
                                   String conversionRate) {
        validateText(campaignName, "Campaign name is required");
        validateText(totalSent, "Total sent is required");
        validateText(qrUsed, "QR used is required");
        validateText(conversionRate, "Conversion rate is required");

        return """
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """.formatted(
                escapeHtml(campaignName),
                escapeHtml(totalSent),
                escapeHtml(qrUsed),
                escapeHtml(conversionRate)
        );
    }

    public String buildCampaignRows(String... rows) {
        if (rows == null || rows.length == 0) {
            throw new ApiException("At least one campaign row is required");
        }
        return String.join("", rows);
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ApiException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String loadMonthlyReportTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(MONTHLY_REPORT_TEMPLATE);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException("Failed to load monthly report template: " + e.getMessage());
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
