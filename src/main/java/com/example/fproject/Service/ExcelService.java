package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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
        if (row == null) {
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
}
