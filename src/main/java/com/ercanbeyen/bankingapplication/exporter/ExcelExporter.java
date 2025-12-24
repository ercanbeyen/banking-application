package com.ercanbeyen.bankingapplication.exporter;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.util.ExporterUtil;
import lombok.experimental.UtilityClass;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@UtilityClass
public class ExcelExporter {
    private final int CENTER_COLUMN_INDEX = 5;
    private int ROW_INDEX_COUNTER = 0;

    public Workbook generateAccountStatementWorkbook(Account account, List<AccountActivityDto> accountActivityDtos) throws IOException {
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

        writeCell(row, columnIndex++, "Time", style, sheet);
        writeCell(row, columnIndex++, "Account Activity", style, sheet);
        writeCell(row, columnIndex, "Amount", style, sheet);
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
            writeCell(row, columnIndex++, accountActivityDto.createdAt().toString(), style, sheet);
            writeCell(row, columnIndex++, accountActivityDto.type().getValue(), style, sheet);
            writeCell(row, columnIndex, ExporterUtil.calculateAmountForDataLine(accountId, accountActivityDto), style, sheet);
        }
    }

    private void writeHeader(String name, Workbook workbook) throws IOException {
        Sheet sheet = workbook.createSheet(name);

        writeHeaderText(workbook, name, HSSFColor.HSSFColorPredefined.DARK_BLUE, ExporterUtil.getBankName());
        ROW_INDEX_COUNTER++;
        writeLogo(workbook, sheet);
        writeHeaderText(workbook, name, HSSFColor.HSSFColorPredefined.DARK_RED, ExporterUtil.getAccountStatementTitle());

        ROW_INDEX_COUNTER += 3;
    }

    private void writeHeaderText(Workbook workbook, String name, HSSFColor.HSSFColorPredefined color, String text) {
        Sheet sheet = workbook.getSheet(name);
        Row row = sheet.createRow(ROW_INDEX_COUNTER);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        font.setColor(color.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);

        writeCell(row, CENTER_COLUMN_INDEX, text, style, sheet);
    }

    private void writeLogo(Workbook workbook, Sheet sheet) throws IOException {
        InputStream inputStream = new FileInputStream(ExporterUtil.getLogoPath());
        byte[] bytes = IOUtils.toByteArray(inputStream);
        int pictureIndex = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);

        XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor();

        anchor.setCol1(CENTER_COLUMN_INDEX);
        anchor.setCol2(CENTER_COLUMN_INDEX + 2);
        anchor.setRow1(ROW_INDEX_COUNTER);
        anchor.setRow2(ROW_INDEX_COUNTER + 1);

        drawing.createPicture(anchor, pictureIndex);
        sheet.autoSizeColumn(ROW_INDEX_COUNTER++);
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

    private void writeCell(Row row, int columnIndex, Object givenValue, CellStyle style, Sheet sheet) {
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
