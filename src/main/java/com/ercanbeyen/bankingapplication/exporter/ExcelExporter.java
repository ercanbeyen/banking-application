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
    private final int CENTER_COLUMN_INDEX = 5;
    private int ROW_INDEX_COUNTER = 0;

    public Workbook generateAccountStatementWorkbook(Account account, List<AccountActivityDto> accountActivityDtos) {
        Workbook workbook = new XSSFWorkbook();
        String name = "Account Activities - " + account.getId();

        writeHeader(name, workbook);
        writeAccountActivityTable(account, accountActivityDtos, name, workbook);
        writeFooter(name, workbook);

        return workbook;
    }

    private void writeAccountActivityTable(Account account, List<AccountActivityDto> accountActivityDtos, String name, Workbook workbook) {
        writeHeaderRow(name, workbook);
        writeDataRows(account.getId(), name, workbook, accountActivityDtos);
        ROW_INDEX_COUNTER++;
    }

    private void writeHeaderRow(String name, Workbook workbook) {
        Sheet sheet = workbook.getSheet(name);
        Row row = sheet.createRow(ROW_INDEX_COUNTER++);

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
        Sheet sheet = workbook.getSheet(name);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        for (AccountActivityDto accountActivityDto : accountActivityDtos) {
            Row row = sheet.createRow(ROW_INDEX_COUNTER++);
            int columnIndex = 0;
            createCell(row, columnIndex++, accountActivityDto.createdAt().toString(), style, sheet);
            createCell(row, columnIndex++, accountActivityDto.type().getValue(), style, sheet);
            createCell(row, columnIndex, ExporterUtil.calculateAmountForDataLine(accountId, accountActivityDto), style, sheet);
        }
    }

    private void writeHeader(String name, Workbook workbook) {
        Sheet sheet = workbook.createSheet(name);
        Row row = sheet.createRow(ROW_INDEX_COUNTER++);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        font.setColor(HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
        style.setFont(font);

        createCell(row, CENTER_COLUMN_INDEX, ExporterUtil.getBankName(), style, sheet);

        row = sheet.createRow(ROW_INDEX_COUNTER);
        style = workbook.createCellStyle();
        font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        font.setColor(HSSFColor.HSSFColorPredefined.DARK_RED.getIndex());
        style.setFont(font);

        createCell(row, CENTER_COLUMN_INDEX, "ACCOUNT STATEMENT", style, sheet);

        ROW_INDEX_COUNTER += 3;
    }

    private void writeFooter(String name, Workbook workbook) {
        Sheet sheet = workbook.getSheet(name);

        Row row = sheet.createRow(ROW_INDEX_COUNTER++);
        Cell cell = row.createCell(0);
        cell.setCellValue(ExporterUtil.getLawMessage());

        row = sheet.createRow(ROW_INDEX_COUNTER);
        cell = row.createCell(0);
        cell.setCellValue(ExporterUtil.getTimeZoneMessage());
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
