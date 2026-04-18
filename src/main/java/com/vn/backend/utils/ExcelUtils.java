package com.vn.backend.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class ExcelUtils {

    /**
     * Xuất Excel chung cho mọi DTO
     * @param data List<T>
     * @param sheetName tên sheet trong file Excel
     * @param headers mảng header tương ứng với field
     * @param fieldNames tên biến trong DTO theo đúng thứ tự cột
     */
    public static <T> ByteArrayResource exportToExcel(
            List<T> data,
            String sheetName,
            String[] headers,
            String[] fieldNames
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            // Style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Tạo hàng header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Ghi dữ liệu từng dòng
            int rowIndex = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < fieldNames.length; col++) {
                    Cell cell = row.createCell(col);
                    Object value = getFieldValue(item, fieldNames[col]);

                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            // Tự động co giãn cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return new ByteArrayResource(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Error exporting excel", e);
        }
    }

    public static Workbook createWorkbook(MultipartFile file) throws IOException {
        return WorkbookFactory.create(file.getInputStream());
    }

    public static Sheet getSheet(Workbook workbook, int sheetIndex) {
        return workbook.getSheetAt(sheetIndex);
    }

    public static Row getRow(Sheet sheet, int rowIndex) {
        return sheet.getRow(rowIndex);
    }

    public Cell getCell(Row row, int cellIndex) {
        return row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    public Double getCellValueAsDouble(Cell cell) {
        try {
            String value = getCellValueAsString(cell);
            return value == null ? null : Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    // Lấy giá trị field qua reflection
    private static Object getFieldValue(Object item, String fieldName) {
        try {
            Field field = item.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(item);
        } catch (Exception e) {
            return null;
        }
    }
}

