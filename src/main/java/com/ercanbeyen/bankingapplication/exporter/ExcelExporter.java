package com.ercanbeyen.bankingapplication.exporter;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.util.ExporterUtil;
import lombok.experimental.UtilityClass;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

@UtilityClass
public class ExcelExporter {
    public Workbook generateAccountActivityWorkbook(Account account, List<AccountActivityDto> accountActivityDtos) {
        Workbook workbook = new XSSFWorkbook();
        String name = "Account Activities - " + account.getId();

        writeHeaderRow(name, workbook);
        writeDataRows(account.getId(), name, workbook, accountActivityDtos);

        return workbook;
    }

    private void writeHeaderRow(String name, Workbook workbook) {
        Sheet sheet = workbook.createSheet(name);
        Row row = workbook.getSheet(name).createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.index);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);

        int columnIndex = 0;

        createCell(row, columnIndex++, "Time", style, sheet);
        createCell(row, columnIndex++, "Account Activity", style, sheet);
        createCell(row, columnIndex, "Amount", style, sheet);
    }

    private void writeDataRows(Integer accountId, String name, Workbook workbook, List<AccountActivityDto> accountActivityDtos) {
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
            createCell(row, columnIndex, ExporterUtil.calculateAmountForDataLine(accountId, accountActivityDto), style, sheet);
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
