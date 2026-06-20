package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.Model.SalesRecordItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleSheetService {

    @Value("${google.sheets.api-key:}")
    private String googleSheetsApiKey;

    @Value("${google.sheets.base-url:https://sheets.googleapis.com/v4}")
    private String googleSheetsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String extractSalesData(String spreadsheetId, String range) {
        List<List<Object>> rows = getSheetRows(spreadsheetId, range);

        StringBuilder salesData = new StringBuilder();
        salesData.append("Source: Google Sheets\n");
        salesData.append("Spreadsheet ID: ").append(spreadsheetId).append("\n");
        salesData.append("Range: ").append(range).append("\n");

        for (List<Object> row : rows) {
            if (isRowEmpty(row)) {
                continue;
            }

            for (int i = 0; i < row.size(); i++) {
                salesData.append(row.get(i));

                if (i < row.size() - 1) {
                    salesData.append(" | ");
                }
            }

            salesData.append("\n");
        }

        return salesData.toString();
    }

    public List<SalesRecordItem> extractSalesRecordItems(String spreadsheetId, String range) {
        List<List<Object>> rows = getSheetRows(spreadsheetId, range);

        Integer productNameColumn = null;
        Integer quantityColumn = null;
        Integer unitPriceColumn = null;
        Integer saleDateColumn = null;
        Integer saleTimeColumn = null;

        Boolean headerFound = false;
        List<SalesRecordItem> salesRecordItems = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<Object> row = rows.get(rowIndex);

            if (isRowEmpty(row)) {
                continue;
            }

            if (!headerFound) {
                for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                    String header = getCellValue(row, cellIndex);

                    if (isProductNameHeader(header)) {
                        productNameColumn = cellIndex;
                    } else if (isQuantityHeader(header)) {
                        quantityColumn = cellIndex;
                    } else if (isUnitPriceHeader(header)) {
                        unitPriceColumn = cellIndex;
                    } else if (isSaleDateHeader(header)) {
                        saleDateColumn = cellIndex;
                    } else if (isSaleTimeHeader(header)) {
                        saleTimeColumn = cellIndex;
                    }
                }

                if (productNameColumn == null
                        || quantityColumn == null
                        || unitPriceColumn == null
                        || saleDateColumn == null
                        || saleTimeColumn == null) {
                    throw new ApiException("Google Sheet must include columns: productName, quantity, unitPrice, saleDate, saleTime");
                }

                headerFound = true;
                continue;
            }

            String productName = getCellValue(row, productNameColumn);

            if (productName.isBlank()) {
                throw new ApiException("Missing product name in Google Sheet row " + (rowIndex + 1));
            }

            Integer quantity = readInteger(getCellValue(row, quantityColumn), "quantity", rowIndex);
            Double unitPrice = readDouble(getCellValue(row, unitPriceColumn), "unitPrice", rowIndex);
            LocalDate saleDate = readLocalDate(getCellValue(row, saleDateColumn), rowIndex);
            LocalTime saleTime = readLocalTime(getCellValue(row, saleTimeColumn), rowIndex);

            if (quantity <= 0) {
                throw new ApiException("Quantity must be greater than zero in Google Sheet row " + (rowIndex + 1));
            }

            if (unitPrice < 0) {
                throw new ApiException("Unit price cannot be negative in Google Sheet row " + (rowIndex + 1));
            }

            SalesRecordItem item = new SalesRecordItem();
            item.setProductName(productName);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(quantity * unitPrice);
            item.setSaleDate(saleDate);
            item.setSaleTime(saleTime);

            salesRecordItems.add(item);
        }

        if (salesRecordItems.isEmpty()) {
            throw new ApiException("Google Sheet does not contain valid sales rows");
        }

        return salesRecordItems;
    }

    private List<List<Object>> getSheetRows(String spreadsheetId, String range) {
        validateGoogleSheetRequest(spreadsheetId, range);

        try {
            String encodedRange = UriUtils.encodePathSegment(range.trim(), StandardCharsets.UTF_8);

            String url = googleSheetsBaseUrl
                    + "/spreadsheets/"
                    + spreadsheetId.trim()
                    + "/values/"
                    + encodedRange
                    + "?key="
                    + googleSheetsApiKey
                    + "&valueRenderOption=FORMATTED_VALUE";

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("values") == null) {
                throw new ApiException("Google Sheet has no values");
            }

            return (List<List<Object>>) response.get("values");

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to read Google Sheet data");
        }
    }

    private void validateGoogleSheetRequest(String spreadsheetId, String range) {
        if (googleSheetsApiKey == null || googleSheetsApiKey.isBlank()) {
            throw new ApiException("Google Sheets API key is missing");
        }

        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            throw new ApiException("Spreadsheet id is required");
        }

        if (range == null || range.isBlank()) {
            throw new ApiException("Sheet range is required");
        }
    }

    private Boolean isRowEmpty(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }

        for (Object cell : row) {
            if (cell != null && !cell.toString().isBlank()) {
                return false;
            }
        }

        return true;
    }

    private String getCellValue(List<Object> row, Integer index) {
        if (row == null || index == null || index >= row.size() || row.get(index) == null) {
            return "";
        }

        return row.get(index).toString().trim();
    }

    private Boolean isProductNameHeader(String header) {
        String value = normalizeHeader(header);

        return value.equals("productname")
                || value.equals("product")
                || value.equals("item")
                || value.equals("name")
                || value.equals("producttitle")
                || value.equals("المنتج")
                || value.equals("اسمالمنتج");
    }

    private Boolean isQuantityHeader(String header) {
        String value = normalizeHeader(header);

        return value.equals("quantity")
                || value.equals("qty")
                || value.equals("units")
                || value.equals("amount")
                || value.equals("الكمية")
                || value.equals("كمية");
    }

    private Boolean isUnitPriceHeader(String header) {
        String value = normalizeHeader(header);

        return value.equals("unitprice")
                || value.equals("price")
                || value.equals("unitcost")
                || value.equals("cost")
                || value.equals("السعر")
                || value.equals("سعر")
                || value.equals("سعرالوحدة");
    }

    private Boolean isSaleDateHeader(String header) {
        String value = normalizeHeader(header);

        return value.equals("saledate")
                || value.equals("date")
                || value.equals("day")
                || value.equals("التاريخ")
                || value.equals("تاريخ");
    }

    private Boolean isSaleTimeHeader(String header) {
        String value = normalizeHeader(header);

        return value.equals("saletime")
                || value.equals("time")
                || value.equals("hour")
                || value.equals("الوقت")
                || value.equals("وقت")
                || value.equals("الساعة")
                || value.equals("ساعة");
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }

        return header.trim()
                .toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private Integer readInteger(String value, String fieldName, Integer rowIndex) {
        try {
            if (value == null || value.isBlank()) {
                throw new ApiException("Missing Google Sheet value");
            }

            value = value.replace(",", "");
            return Integer.parseInt(value);

        } catch (Exception e) {
            throw new ApiException("Invalid Google Sheet number value: " + fieldName + " in row " + (rowIndex + 1));
        }
    }

    private Double readDouble(String value, String fieldName, Integer rowIndex) {
        try {
            if (value == null || value.isBlank()) {
                throw new ApiException("Missing Google Sheet value");
            }

            value = value.replace(",", "");
            value = value.replace("SAR", "");
            value = value.replace("ريال", "");
            value = value.trim();

            return Double.parseDouble(value);

        } catch (Exception e) {
            throw new ApiException("Invalid Google Sheet decimal value: " + fieldName + " in row " + (rowIndex + 1));
        }
    }

    private LocalDate readLocalDate(String value, Integer rowIndex) {
        try {
            if (value == null || value.isBlank()) {
                throw new ApiException("Missing Google Sheet sale date");
            }

            value = value.trim();

            try {
                return LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ignored) {
            }

            try {
                return LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy/M/d"));
            } catch (Exception ignored) {
            }

            try {
                return LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy"));
            } catch (Exception ignored) {
            }

            return LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));

        } catch (Exception e) {
            throw new ApiException("Invalid Google Sheet sale date in row " + (rowIndex + 1));
        }
    }

    private LocalTime readLocalTime(String value, Integer rowIndex) {
        try {
            if (value == null || value.isBlank()) {
                throw new ApiException("Missing Google Sheet sale time");
            }

            value = value.trim();
            value = value.replace("：", ":");
            value = value.replace("ص", "AM");
            value = value.replace("م", "PM");
            value = value.toUpperCase();

            if (value.contains("AM") || value.contains("PM")) {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH);

                return LocalTime.parse(value, formatter);
            }

            if (value.matches("\\d{1,2}:\\d{2}")) {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("H:mm");

                return LocalTime.parse(value, formatter);
            }

            if (value.matches("\\d{1,2}")) {
                return LocalTime.of(Integer.parseInt(value), 0);
            }

            return LocalTime.parse(value);

        } catch (Exception e) {
            throw new ApiException("Invalid Google Sheet sale time in row " + (rowIndex + 1));
        }
    }
}
