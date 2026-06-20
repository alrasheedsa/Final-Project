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

    private static final String PRIMARY      = "#1a5c5b";
    private static final String PRIMARY_LIGHT = "#e8f4f4";
    private static final String ACCENT       = "#f0e8d8";
    private static final String TEXT_DARK    = "#1a3a3a";
    private static final String TEXT_MUTED   = "#5a7a7a";
    private static final String BORDER       = "#d4e6e6";
    private static final String WHITE        = "#ffffff";

    // ─────────────────────────────────────────────
    // Sales Monthly Report PDF
    // ─────────────────────────────────────────────

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

        String monthLabel = monthName(month) + " " + year;

        String css =
            "@page { size: A4; margin: 0; }" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }" +
            "body { font-family: Arial, sans-serif; color: " + TEXT_DARK + "; background: " + WHITE + "; }" +
            ".header { background: " + PRIMARY + "; padding: 28px 40px 24px; display: flex; justify-content: space-between; align-items: center; }" +
            ".logo-text { color: " + WHITE + "; font-size: 22px; font-weight: bold; }" +
            ".report-badge { background: rgba(255,255,255,0.2); color: " + WHITE + "; padding: 5px 14px; border-radius: 20px; font-size: 12px; }" +
            ".hero { background: " + PRIMARY_LIGHT + "; padding: 20px 40px; border-bottom: 1px solid " + BORDER + "; }" +
            ".hero h1 { font-size: 20px; font-weight: bold; color: " + PRIMARY + "; margin-bottom: 6px; }" +
            ".hero .meta { font-size: 12px; color: " + TEXT_MUTED + "; }" +
            ".content { padding: 28px 40px; }" +
            ".stats-row { margin-bottom: 28px; display: flex; gap: 16px; }" +
            ".stat-card { background: " + WHITE + "; border: 1px solid " + BORDER + "; border-radius: 10px; padding: 16px 20px; text-align: center; flex: 1; }" +
            ".stat-card .value { font-size: 22px; font-weight: bold; color: " + PRIMARY + "; margin-bottom: 4px; }" +
            ".stat-card .label { font-size: 11px; color: " + TEXT_MUTED + "; }" +
            ".section-title { font-size: 14px; font-weight: bold; color: " + PRIMARY + "; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid " + PRIMARY_LIGHT + "; }" +
            "table { width: 100%; border-collapse: collapse; margin-bottom: 28px; font-size: 12px; }" +
            "thead tr { background: " + PRIMARY + "; color: " + WHITE + "; }" +
            "thead td { padding: 10px 14px; font-weight: bold; font-size: 11px; }" +
            "tbody tr:nth-child(even) { background: " + PRIMARY_LIGHT + "; }" +
            "tbody tr:nth-child(odd) { background: " + WHITE + "; }" +
            "tbody td { padding: 10px 14px; border-bottom: 1px solid " + BORDER + "; color: " + TEXT_DARK + "; vertical-align: top; }" +
            "tbody td:first-child { font-weight: bold; color: " + TEXT_DARK + "; width: 180px; }" +
            ".summary-box { background: " + PRIMARY_LIGHT + "; border: 1px solid " + BORDER + "; border-left: 4px solid " + PRIMARY + "; border-radius: 8px; padding: 18px 20px; font-size: 13px; line-height: 1.8; color: " + TEXT_DARK + "; margin-bottom: 28px; }" +
            ".footer { background: " + PRIMARY + "; padding: 16px 40px; font-size: 10px; color: rgba(255,255,255,0.7); display: flex; justify-content: space-between; }" +
            ".footer .brand { color: " + WHITE + "; font-weight: bold; }";

        String html =
            "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\"/>" +
            "<style>" + css + "</style>" +
            "</head>" +
            "<body>" +

            "<div class=\"header\">" +
            "<span class=\"logo-text\">Ala Darbak</span>" +
            "<span class=\"report-badge\">Monthly Sales Report</span>" +
            "</div>" +

            "<div class=\"hero\">" +
            "<h1>" + escapeHtml(storeName) + " - " + escapeHtml(branchName) + "</h1>" +
            "<div class=\"meta\">" + monthLabel + "</div>" +
            "</div>" +

            "<div class=\"content\">" +

            "<div class=\"stats-row\">" +
            "<div class=\"stat-card\"><div class=\"value\">" + String.format("%.2f", totalSales) + "</div><div class=\"label\">Total Sales (SAR)</div></div>" +
            "<div class=\"stat-card\"><div class=\"value\">" + totalQuantity + "</div><div class=\"label\">Total Units Sold</div></div>" +
            "<div class=\"stat-card\"><div class=\"value\">" + escapeHtml(peakHours) + "</div><div class=\"label\">Peak Hour</div></div>" +
            "</div>" +

            "<div class=\"section-title\">Sales Details</div>" +
            "<table>" +
            "<thead><tr><td>Item</td><td>Value</td></tr></thead>" +
            "<tbody>" +
            "<tr><td>Top Products</td><td>" + escapeHtml(topProducts) + "</td></tr>" +
            "<tr><td>Low Products</td><td>" + escapeHtml(lowProducts) + "</td></tr>" +
            "<tr><td>Peak Hour</td><td>" + escapeHtml(peakHours) + "</td></tr>" +
            "<tr><td>Slow Hour</td><td>" + escapeHtml(slowHours) + "</td></tr>" +
            "</tbody>" +
            "</table>" +

            "<div class=\"section-title\">AI Analysis</div>" +
            "<div class=\"summary-box\">" + escapeHtml(summary) + "</div>" +

            "</div>" +

            "<div class=\"footer\">" +
            "<span class=\"brand\">Ala Darbak</span>" +
            "<span>Connecting users with nearby smart offers</span>" +
            "<span>2026 All rights reserved</span>" +
            "</div>" +

            "</body></html>";

        return renderHtmlToPdf(html);
    }

    // ─────────────────────────────────────────────
    // Campaign Monthly Report PDF (existing)
    // ─────────────────────────────────────────────

    public byte[] generateMonthlyReportPdf(String storeName,
                                           String generatedAt,
                                           String campaignRows,
                                           String recommendations) {
        validateText(storeName,       "Store name is required");
        validateText(generatedAt,     "Report date is required");
        validateText(campaignRows,    "Campaign rows are required");
        validateText(recommendations, "Recommendations are required");

        String html = loadMonthlyReportTemplate()
                .replace("{{storeName}}",       escapeHtml(storeName))
                .replace("{{generatedAt}}",     escapeHtml(generatedAt))
                .replace("{{campaignRows}}",    campaignRows)
                .replace("{{recommendations}}", escapeHtml(recommendations));

        return renderHtmlToPdf(html);
    }

    public byte[] generateMonthlyReportPdf(String reportContent) {
        validateText(reportContent, "Report content is required");
        String row = buildCampaignRow("ملخص التقرير", "0", "0", "0%");
        return generateMonthlyReportPdf("على دربك", "غير محدد", row, reportContent);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    public String buildCampaignRow(String campaignName,
                                   String totalSent,
                                   String qrUsed,
                                   String conversionRate) {
        validateText(campaignName,   "Campaign name is required");
        validateText(totalSent,      "Total sent is required");
        validateText(qrUsed,         "QR used is required");
        validateText(conversionRate, "Conversion rate is required");

        return "<tr>" +
               "<td>" + escapeHtml(campaignName)   + "</td>" +
               "<td>" + escapeHtml(totalSent)       + "</td>" +
               "<td>" + escapeHtml(qrUsed)          + "</td>" +
               "<td>" + escapeHtml(conversionRate)  + "</td>" +
               "</tr>";
    }

    public String buildCampaignRows(String... rows) {
        if (rows == null || rows.length == 0) {
            throw new ApiException("At least one campaign row is required");
        }
        return String.join("", rows);
    }

    private String monthName(Integer month) {
        return switch (month) {
            case 1  -> "January";
            case 2  -> "February";
            case 3  -> "March";
            case 4  -> "April";
            case 5  -> "May";
            case 6  -> "June";
            case 7  -> "July";
            case 8  -> "August";
            case 9  -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> String.valueOf(month);
        };
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
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}