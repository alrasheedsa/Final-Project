package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.Model.SalesRecordItem;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    public String extractSalesData(MultipartFile file) {
        validateExcelFile(file);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            DataFormatter formatter = new DataFormatter();
            StringBuilder salesData = new StringBuilder();

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                salesData.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                for (Row row : sheet) {
                    if (isRowEmpty(row, formatter)) {
                        continue;
                    }

                    for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        salesData.append(formatter.formatCellValue(cell));

                        if (cellIndex < row.getLastCellNum() - 1) {
                            salesData.append(" | ");
                        }
                    }

                    salesData.append("\n");
                }
            }

            if (salesData.toString().isBlank()) {
                throw new ApiException("Excel file does not contain sales data");
            }

            return salesData.toString();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to read Excel sales file");
        }
    }

    public List<SalesRecordItem> extractSalesRecordItems(MultipartFile file) {
        validateExcelFile(file);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            DataFormatter formatter = new DataFormatter();
            List<SalesRecordItem> salesRecordItems = new ArrayList<>();

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);

                Integer productNameColumn = null;
                Integer quantityColumn = null;
                Integer unitPriceColumn = null;
                Integer saleDateColumn = null;
                Integer saleTimeColumn = null;
                Boolean headerFound = false;

                for (Row row : sheet) {
                    if (isRowEmpty(row, formatter)) {
                        continue;
                    }

                    if (!headerFound) {
                        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                            String header = formatter.formatCellValue(row.getCell(cellIndex)).trim();

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

                        if (productNameColumn == null || quantityColumn == null || unitPriceColumn == null
                                || saleDateColumn == null || saleTimeColumn == null) {
                            throw new ApiException("Excel headers must include productName, quantity, unitPrice, saleDate, saleTime");
                        }

                        headerFound = true;
                        continue;
                    }

                    String productName = formatter.formatCellValue(row.getCell(productNameColumn)).trim();

                    if (productName.isBlank()) {
                        continue;
                    }

                    Integer quantity = readInteger(row.getCell(quantityColumn), formatter, "quantity", row.getRowNum());
                    Double unitPrice = readDouble(row.getCell(unitPriceColumn), formatter, "unitPrice", row.getRowNum());
                    LocalDate saleDate = readLocalDate(row.getCell(saleDateColumn), formatter, row.getRowNum());
                    LocalTime saleTime = readLocalTime(row.getCell(saleTimeColumn), formatter, row.getRowNum());

                    if (quantity <= 0) {
                        throw new ApiException("Quantity must be greater than zero in Excel row " + (row.getRowNum() + 1));
                    }

                    if (unitPrice < 0) {
                        throw new ApiException("Unit price cannot be negative in Excel row " + (row.getRowNum() + 1));
                    }

                    SalesRecordItem salesRecordItem = new SalesRecordItem();
                    salesRecordItem.setProductName(productName);
                    salesRecordItem.setQuantity(quantity);
                    salesRecordItem.setUnitPrice(unitPrice);
                    salesRecordItem.setTotalPrice(quantity * unitPrice);
                    salesRecordItem.setSaleDate(saleDate);
                    salesRecordItem.setSaleTime(saleTime);

                    salesRecordItems.add(salesRecordItem);
                }
            }

            if (salesRecordItems.isEmpty()) {
                throw new ApiException("Excel file does not contain valid sales rows");
            }

            return salesRecordItems;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to extract sales record items from Excel file");
        }
    }

    public void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Excel sales file is required");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new ApiException("Excel file name is required");
        }

        String lowerFileName = fileName.toLowerCase();

        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            throw new ApiException("Sales record must be uploaded as an Excel file");
        }
    }

    private Boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null || row.getLastCellNum() <= 0) {
            return true;
        }

        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex);

            if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }

        return true;
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

    private Integer readInteger(Cell cell, DataFormatter formatter, String fieldName, Integer rowIndex) {
        if (cell == null) {
            throw new ApiException("Missing Excel value: " + fieldName + " in row " + (rowIndex + 1));
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }

            String value = formatter.formatCellValue(cell).trim().replace(",", "");
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ApiException("Invalid Excel number value: " + fieldName + " in row " + (rowIndex + 1));
        }
    }

    private Double readDouble(Cell cell, DataFormatter formatter, String fieldName, Integer rowIndex) {
        if (cell == null) {
            throw new ApiException("Missing Excel value: " + fieldName + " in row " + (rowIndex + 1));
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }

            String value = formatter.formatCellValue(cell).trim().replace(",", "");
            return Double.parseDouble(value);
        } catch (Exception e) {
            throw new ApiException("Invalid Excel decimal value: " + fieldName + " in row " + (rowIndex + 1));
        }
    }

    private LocalDate readLocalDate(Cell cell, DataFormatter formatter, Integer rowIndex) {
        if (cell == null) {
            throw new ApiException("Missing Excel sale date in row " + (rowIndex + 1));
        }

        try {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            String value = formatter.formatCellValue(cell).trim();

            DateTimeFormatter formatterOne = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter formatterTwo = DateTimeFormatter.ofPattern("yyyy/M/d");
            DateTimeFormatter formatterThree = DateTimeFormatter.ofPattern("M/d/yyyy");
            DateTimeFormatter formatterFour = DateTimeFormatter.ofPattern("d/M/yyyy");

            try {
                return LocalDate.parse(value, formatterOne);
            } catch (Exception ignored) {
            }

            try {
                return LocalDate.parse(value, formatterTwo);
            } catch (Exception ignored) {
            }

            try {
                return LocalDate.parse(value, formatterThree);
            } catch (Exception ignored) {
            }

            return LocalDate.parse(value, formatterFour);
        } catch (Exception e) {
            throw new ApiException("Invalid Excel sale date in row " + (rowIndex + 1));
        }
    }

    private LocalTime readLocalTime(Cell cell, DataFormatter formatter, Integer rowIndex) {
        if (cell == null) {
            return LocalTime.of(12, 0);
        }

        try {
            String value = formatter.formatCellValue(cell);

            if (value == null || value.isBlank()) {
                return LocalTime.of(12, 0);
            }

            value = value.trim();
            value = value.replace("：", ":");
            value = value.replace("ص", "AM");
            value = value.replace("م", "PM");

            if (value.contains(":")) {
                String[] parts = value.split(":");

                String hourText = parts[0].replaceAll("[^0-9]", "");
                String minuteText = parts[1].replaceAll("[^0-9]", "");

                if (hourText.isBlank() || minuteText.isBlank()) {
                    return LocalTime.of(12, 0);
                }

                Integer hour = Integer.parseInt(hourText);
                Integer minute = Integer.parseInt(minuteText);

                String upperValue = value.toUpperCase();

                if (upperValue.contains("PM") && hour < 12) {
                    hour = hour + 12;
                }

                if (upperValue.contains("AM") && hour == 12) {
                    hour = 0;
                }

                return LocalTime.of(hour, minute).withSecond(0).withNano(0);
            }

            return LocalTime.of(12, 0);

        } catch (Exception e) {
            return LocalTime.of(12, 0);
        }
    }}
