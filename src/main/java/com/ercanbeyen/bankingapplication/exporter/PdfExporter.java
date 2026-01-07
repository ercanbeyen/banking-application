package com.ercanbeyen.bankingapplication.exporter;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.event.BorderEvent;
import com.ercanbeyen.bankingapplication.event.PageNumerationEvent;
import com.ercanbeyen.bankingapplication.util.ExporterUtil;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

@Slf4j
@UtilityClass
public class PdfExporter {
    private final List<String> customerCredentials = List.of(SummaryField.FULL_NAME, SummaryField.NATIONAL_IDENTITY);

    public ByteArrayOutputStream generatePdfStreamOfFinancialStatusReport(Customer customer) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPageEvent(new PageNumerationEvent());
        document.open();

        writeHeader(document);
        writeTitle(document, "FINANCIAL STATUS");
        addNewLine(document);

        writeFinancialStatusReportBody(document, customer);
        addNewLine(document);

        writeFooter(document);
        document.close();

        return outputStream;
    }

    public ByteArrayOutputStream generatePdfStreamOfReceipt(AccountActivity accountActivity) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        writeHeader(document);
        writeTitle(document, "RECEIPT");
        addNewLine(document);

        writeReceiptBody(document, accountActivity);
        addNewLine(document);

        writeFooter(document);
        document.close();

        return outputStream;
    }

    public ByteArrayOutputStream generateAccountStatementPdf(Account account, LocalDate fromDate, LocalDate toDate, List<AccountActivityDto> accountActivityDtos) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPageEvent(new PageNumerationEvent());
        document.open();

        writeHeader(document);
        writeTitle(document, ExporterUtil.getAccountStatementTitle());
        addNewLine(document);

        writeAccountStatementBody(account, fromDate, toDate, accountActivityDtos, document);

        writeFooter(document);
        document.close();

        return outputStream;
    }

    private void writeTitle(Document document, String title) throws DocumentException {
        Font font = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, BaseColor.RED);

        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);

        document.add(paragraph);
    }

    private void writeFinancialStatusReportBody(Document document, Customer customer) throws DocumentException {
        final Font font = new Font();
        font.setStyle(Font.BOLD);

        Chunk fullNameInputChunk = new Chunk("Full Name: ", font);
        Chunk fullNameOutputChunk = new Chunk(customer.getFullName());

        Chunk dateInputChunk = new Chunk("  Date: ", font);
        LocalDateTime today = TimeUtil.getCurrentTimeStampInTurkey();
        String todayDate = today.toLocalDate().toString();
        LocalTime todayLocalTime = today.toLocalTime();
        String todayTime = todayLocalTime.getHour() + ":" + todayLocalTime.getMinute();

        Chunk dateOutputChunk = new Chunk(todayDate + " " + todayTime);

        Phrase phrase = new Phrase();
        phrase.addAll(List.of(fullNameInputChunk, fullNameOutputChunk, dateInputChunk, dateOutputChunk));

        Paragraph paragraph = new Paragraph(phrase);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        addNewLine(document);

        PdfPTable table = new PdfPTable(2);
        Map<List<Enum<? extends Enum<?>>>, Double> accountFinancialStatuses = new HashMap<>();

        for (Account account : customer.getAccounts()) {
            List<Enum<? extends Enum<?>>> key = List.of(account.getType(), account.getCurrency());
            double balance = account.getBalance();
            accountFinancialStatuses.computeIfPresent(key, (_, value) -> value + balance);
            accountFinancialStatuses.putIfAbsent(key, balance);
        }

        for (Map.Entry<List<Enum<? extends Enum<?>>>, Double> entry : accountFinancialStatuses.entrySet()) {
            List<Enum<? extends Enum<?>>> key = entry.getKey();
            AccountType accountType = AccountType.valueOf(key.getFirst().toString());
            table.addCell(new PdfPCell(new Phrase(accountType.getValue() + " " + key.getLast() + " " + Entity.ACCOUNT.getValue())));
            table.addCell(new PdfPCell(new Phrase(entry.getValue().toString())));
        }

        document.add(table);

    }

    private void writeAccountStatementBody(Account account, LocalDate fromDate, LocalDate toDate, List<AccountActivityDto> accountActivityDtos, Document document) throws DocumentException {
        writeInformationTable(document, account, fromDate, toDate);
        addNewLine(document);

        writeAccountActivityTable(account, document, accountActivityDtos);
        addNewLine(document);
    }

    private void writeReceiptBody(Document document, AccountActivity accountActivity) throws DocumentException {
        PdfPTable table = new PdfPTable(2);

        /* Header row */
        writeHeaderRowOfActivityTable(List.of("Field", "Value"), table);

        /* Data rows */
        for (Map.Entry<String, Object> entry : accountActivity.getSummary().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();

            if (customerCredentials.contains(key)) {
                value = maskField(entry);
            }

            table.addCell(key);
            table.addCell(value);
        }

        document.add(table);
    }

    private void writeInformationTable(Document document, Account account, LocalDate fromDate, LocalDate toDate) throws DocumentException {
        BorderEvent borderEvent = new BorderEvent();

        PdfPTable table = new PdfPTable(2);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        table.setTableEvent(borderEvent);

        PdfPTable accountInformationTable = new PdfPTable(1);
        accountInformationTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        accountInformationTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        accountInformationTable.setTableEvent(borderEvent);

        Customer customer = account.getCustomer();
        Map.Entry<String, Object> entry = Map.entry(SummaryField.NATIONAL_IDENTITY, customer.getNationalId());

        accountInformationTable.addCell("Dear " + customer.getName().toUpperCase());
        accountInformationTable.addCell(new Phrase(new Paragraph("\n")));
        accountInformationTable.addCell("Customer Number: " + customer.getId());
        accountInformationTable.addCell("Customer National Identity Number: " + maskField(entry));
        accountInformationTable.addCell("Branch: " + account.getBranch().getName());
        accountInformationTable.addCell("Account Identity: " + account.getId());
        accountInformationTable.addCell("Account Type: " + account.getType());
        accountInformationTable.addCell("Currency: " + account.getCurrency());
        accountInformationTable.addCell("Balance: " + account.getBalance());

        table.addCell(accountInformationTable);

        PdfPTable transactionInformationTable = new PdfPTable(1);
        transactionInformationTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        transactionInformationTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        transactionInformationTable.setTableEvent(borderEvent);

        transactionInformationTable.addCell("Document Issue Date: " + LocalDate.now());
        transactionInformationTable.addCell("Inquiry Criteria: " + fromDate + " - " + toDate);

        table.addCell(transactionInformationTable);

        document.add(table);
    }

    private void writeAccountActivityTable(Account account, Document document, List<AccountActivityDto> accountActivityDtos) throws DocumentException {
        final int numberOfColumns = 3;
        PdfPTable table = new PdfPTable(numberOfColumns);

        /* Header row */
        writeHeaderRowOfActivityTable(List.of("Time", "Account Activity", "Amount"), table);

        /* Data rows */
        for (AccountActivityDto accountActivityDto : accountActivityDtos) {
            table.addCell(new PdfPCell(new Phrase(accountActivityDto.createdAt().toString())));
            table.addCell(new PdfPCell(new Phrase(accountActivityDto.type().getValue())));
            table.addCell(new PdfPCell(new Phrase(ExporterUtil.calculateAmountForDataLine(account.getId(), accountActivityDto).toString())));
        }

        document.add(table);
    }

    private void writeHeaderRowOfActivityTable(List<String> fields, PdfPTable table) {
        final Font font = new Font(Font.FontFamily.HELVETICA, Font.DEFAULTSIZE, Font.BOLD, BaseColor.WHITE);
        fields.forEach(title -> {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(BaseColor.BLUE);
            header.setBorderWidth(1);
            Phrase phrase = new Phrase(title, font);
            header.setPhrase(phrase);
            table.addCell(header);
        });
    }

    private String maskField(Map.Entry<String, Object> entry) {
        String key = entry.getKey();
        String value = entry.getValue().toString();
        StringBuilder valueBuilder = new StringBuilder();

        if (key.equals(SummaryField.FULL_NAME)) {
            int spaceIndex = value.indexOf(' ');
            String name = value.substring(0, spaceIndex);
            String surname = value.substring(spaceIndex + 1);

            valueBuilder.append(maskWordInFullName(name))
                    .append(" ")
                    .append(maskWordInFullName(surname));
        } else if (key.equals(SummaryField.NATIONAL_IDENTITY)) {
            int length = value.length();
            valueBuilder.append(value, 0, 3)
                    .append("*".repeat(length - 5))
                    .append(value, length - 2, length);
        } else {
            throw new ResourceConflictException(String.format("Summary field %s is not in %s", key, customerCredentials));
        }

        return valueBuilder.toString();
    }

    private StringBuilder maskWordInFullName(String word) {
        int length = word.length();
        int endIndex = length < 5 ? 1 : 2;
        log.info("Length and end index: {} & {}", length, endIndex);

        return new StringBuilder()
                .append(word, 0, endIndex)
                .append("*".repeat(length - endIndex));
    }

    private void writeHeader(Document document) throws DocumentException, IOException {
        Font font = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLUE);

        Paragraph paragraph = new Paragraph(ExporterUtil.getBankName(), font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
        writeLogo(document);
    }

    private void writeFooter(Document document) throws DocumentException {
        Font font = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC);

        Paragraph paragraph = new Paragraph(ExporterUtil.getLawMessage(), font);

        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        paragraph = new Paragraph(ExporterUtil.getTimeZoneMessage(), font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
    }

    private void writeLogo(Document document) throws DocumentException, IOException {
        Image image = Image.getInstance(Paths.get(ExporterUtil.getLogoPath())
                .toAbsolutePath()
                .toString());
        image.scalePercent(10);
        image.setAlignment(Element.ALIGN_CENTER);
        document.add(image);
    }

    private void addNewLine(Document document) throws DocumentException {
        Paragraph paragraph = new Paragraph("\n");
        document.add(paragraph);
    }
}
