package com.ercanbeyen.bankingapplication.exporter;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Customer;
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
import java.time.LocalDate;
import java.util.List;

@UtilityClass
public class ExcelExporter {
    private final int BEGINNING_INDEX = 0;
    private final int CENTER_COLUMN_INDEX = 1;
    private int rowIndex = 0;

    public Workbook generateAccountStatementWorkbook(Account account, List<AccountActivityDto> accountActivityDtos, LocalDate fromDate, LocalDate toDate) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        String name = "Account Activities - " + account.getId();

        writeHeader(name, workbook);
        rowIndex += 3;

        writeInformationTable(name, workbook, account, fromDate, toDate);
        rowIndex += 3;

        writeAccountActivityTable(account, accountActivityDtos, name, workbook);
        rowIndex++;

        writeFooter(name, workbook);

        return workbook;
    }

    private void writeAccountActivityTable(Account account, List<AccountActivityDto> accountActivityDtos, String name, Workbook workbook) {
        writeHeaderRow(name, workbook);
        writeDataRows(account.getId(), name, workbook, accountActivityDtos);
    }

    private void writeHeaderRow(String name, Workbook workbook) {
        Sheet sheet = workbook.getSheet(name);
        Row row = sheet.createRow(rowIndex++);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.index);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);

        int columnIndex = BEGINNING_INDEX;

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
            Row row = sheet.createRow(rowIndex++);
            int columnIndex = BEGINNING_INDEX;
            writeCell(row, columnIndex++, accountActivityDto.createdAt().toString(), style, sheet);
            writeCell(row, columnIndex++, accountActivityDto.type().getValue(), style, sheet);
            writeCell(row, columnIndex, ExporterUtil.calculateAmountForDataLine(accountId, accountActivityDto), style, sheet);
        }
    }

    private void writeHeader(String name, Workbook workbook) throws IOException {
        Sheet sheet = workbook.createSheet(name);

        writeHeaderText(workbook, name, HSSFColor.HSSFColorPredefined.DARK_BLUE, ExporterUtil.getBankName());
        rowIndex++;
        writeLogo(workbook, sheet);
        writeHeaderText(workbook, name, HSSFColor.HSSFColorPredefined.DARK_RED, ExporterUtil.getAccountStatementTitle());
    }

    private void writeHeaderText(Workbook workbook, String name, HSSFColor.HSSFColorPredefined color, String text) {
        Sheet sheet = workbook.getSheet(name);
        Row row = sheet.createRow(rowIndex);

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
        anchor.setCol2(CENTER_COLUMN_INDEX + 1);
        anchor.setRow1(rowIndex);
        anchor.setRow2(rowIndex + 1);

        drawing.createPicture(anchor, pictureIndex);
        sheet.autoSizeColumn(rowIndex++);
    }

    private void writeInformationTable(String name, Workbook workbook, Account account, LocalDate fromDate, LocalDate toDate) {
        Sheet sheet = workbook.getSheet(name);
        Customer customer = account.getCustomer();

        final int fieldColumnIndexOfAccountInformationTable = BEGINNING_INDEX;
        final int valueColumnIndexOfAccountInformationTable = BEGINNING_INDEX + 1;
        final int fieldColumnIndexOfTransactionInformationTable = BEGINNING_INDEX + 2;
        final int valueColumnIndexOfTransactionInformationTable = BEGINNING_INDEX + 3;

        CellStyle fieldColumnStyle = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setFontHeight(14);
        font.setBold(true);
        fieldColumnStyle.setFont(font);

        CellStyle valueColumnStyle = workbook.createCellStyle();
        font = (XSSFFont) workbook.createFont();
        font.setFontHeight(14);
        valueColumnStyle.setFont(font);

        Row row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Dear " + customer.getName().toUpperCase(), fieldColumnStyle, sheet);

        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Customer Number:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, account.getId(), valueColumnStyle, sheet);

        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Customer National Identity Number:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, customer.getNationalId(), valueColumnStyle, sheet);
        writeCell(row, fieldColumnIndexOfTransactionInformationTable, "Document Issue Date:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfTransactionInformationTable, LocalDate.now().toString(), valueColumnStyle, sheet);

        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Branch:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, account.getBranch().getName(), valueColumnStyle, sheet);
        writeCell(row, fieldColumnIndexOfTransactionInformationTable, "Inquiry Criteria:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfTransactionInformationTable, fromDate + " - " + toDate, valueColumnStyle, sheet);

        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Account Identity:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, account.getId(), valueColumnStyle, sheet);

        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Account Type:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, account.getId(), valueColumnStyle, sheet);


        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Currency:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, account.getCurrency().toString(), valueColumnStyle, sheet);

        row = sheet.createRow(rowIndex++);
        writeCell(row, fieldColumnIndexOfAccountInformationTable, "Balance:", fieldColumnStyle, sheet);
        writeCell(row, valueColumnIndexOfAccountInformationTable, account.getBalance(), valueColumnStyle, sheet);
    }

    private void writeFooter(String name, Workbook workbook) {
        Sheet sheet = workbook.getSheet(name);

        Row row = sheet.createRow(rowIndex++);
        Cell cell = row.createCell(BEGINNING_INDEX);
        cell.setCellValue(ExporterUtil.getLawMessage());

        row = sheet.createRow(rowIndex);
        cell = row.createCell(BEGINNING_INDEX);
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
