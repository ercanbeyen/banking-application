package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

@UtilityClass
public class ExcelUtil {
    public Workbook generateWorkbook(Account account, List<AccountActivityDto> accountActivityDtos) {
        Workbook workbook = new XSSFWorkbook();
        String name = "Account Activities - " + account.getId();

        writeHeaderLine(name, workbook);
        writeDataLines(name, workbook, accountActivityDtos);

        return workbook;
    }

    private void writeHeaderLine(String name, Workbook workbook) {
        Sheet sheet = workbook.createSheet(name);
        Row row = workbook.getSheet(name).createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);

        int columnIndex = 0;

        createCell(row, columnIndex++, "Time", style, sheet);
        createCell(row, columnIndex++, "Account Activity", style, sheet);
        createCell(row, columnIndex, "Amount", style, sheet);
    }

    private void writeDataLines(String name, Workbook workbook, List<AccountActivityDto> accountActivityDtos) {
        int rowIndex = 1;
        Sheet sheet = workbook.getSheet(name);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        for (AccountActivityDto accountActivityDto : accountActivityDtos) {
            Row row = sheet.createRow(rowIndex++);
            int columnIndex = 0;
            createCell(row, columnIndex++, accountActivityDto.createdAt().toString(), style, sheet);
            createCell(row, columnIndex++, accountActivityDto.type().getValue(), style, sheet);
            createCell(row, columnIndex, accountActivityDto.amount(), style, sheet);
        }
    }

    private void createCell(Row row, int columnIndex, Object givenValue, CellStyle style, Sheet sheet) {
        sheet.autoSizeColumn(columnIndex);
        Cell cell = row.createCell(columnIndex);

        switch (givenValue) {
            case Integer value -> cell.setCellValue(value);
            case Double value -> cell.setCellValue(value);
            case Boolean value -> cell.setCellValue(value);
            default -> cell.setCellValue((String) givenValue);
        }

        cell.setCellStyle(style);
    }
}
