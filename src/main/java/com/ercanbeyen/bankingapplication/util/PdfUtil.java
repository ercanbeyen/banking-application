package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.helper.BorderEvent;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class PdfUtil {
    private static final List<String> customerCredentials = List.of(SummaryField.FULL_NAME, SummaryField.NATIONAL_IDENTITY);

    public ByteArrayOutputStream generatePdfStreamOfReceipt(Map<String, Object> summary) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        addTitle(document, "Receipt");
        document.add(new Paragraph("\n"));

        addTableOfReceipt(document, summary);
        document.add(new Paragraph("\n"));

        addBottom(document);
        document.close();

        return outputStream;
    }

    public ByteArrayOutputStream generatePdfStreamOfStatement(Account account, LocalDate fromDate, LocalDate toDate, List<Map<String, Object>> summaries) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        addTitle(document, "Account Statement");
        document.add(new Paragraph("\n"));

        addTableOfAccountInformation(document, account, fromDate, toDate);
        document.add(new Paragraph("\n"));

        addTableOfAccountActivities(document, summaries);
        document.add(new Paragraph("\n"));

        addBottom(document);
        document.close();

        return outputStream;
    }

    private static void addTitle(Document document, String title) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, BaseColor.RED);

        Paragraph paragraph = new Paragraph(title.toUpperCase(), boldFont);
        paragraph.setAlignment(Element.ALIGN_CENTER);

        document.add(paragraph);
    }

    private static void addTableOfReceipt(Document document, Map<String, Object> summary) throws DocumentException {
        PdfPTable table = new PdfPTable(2);

        Stream.of("Field", "Value")
                .forEach(title -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.CYAN);
                    header.setBorderWidth(1);
                    header.setPhrase(new Phrase(title));
                    table.addCell(header);
                });

        for (Map.Entry<String, Object> entry : summary.entrySet()) {
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

    private static void addTableOfAccountInformation(Document document, Account account, LocalDate fromDate, LocalDate toDate) throws DocumentException {
        BorderEvent borderEvent = new BorderEvent();

        PdfPTable table = new PdfPTable(2);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        table.setTableEvent(borderEvent);

        PdfPTable leftTable = new PdfPTable(1);
        leftTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        leftTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        leftTable.setTableEvent(borderEvent);

        Customer customer = account.getCustomer();
        leftTable.addCell("Dear " + customer.getName().toUpperCase());
        leftTable.addCell("Customer National Identity: " + customer.getNationalId());
        leftTable.addCell("Branch: " + account.getBranch().getName());
        leftTable.addCell("Account Identity: " + account.getId());
        leftTable.addCell("Account Type: " + account.getType());
        leftTable.addCell("Currency: " + account.getCurrency());
        leftTable.addCell("Balance: " + account.getBalance());

        table.addCell(leftTable);

        PdfPTable rightTable = new PdfPTable(1);
        rightTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        rightTable.setTableEvent(borderEvent);

        rightTable.addCell("Document Issue Date: " + LocalDate.now());
        rightTable.addCell("Inquiry Criteria: " + fromDate + " - " + toDate);

        table.addCell(rightTable);

        document.add(table);
    }

    private static void addTableOfAccountActivities(Document document, List<Map<String, Object>> summaries) throws DocumentException {
        final int numberOfColumns = 3;
        PdfPTable table = new PdfPTable(numberOfColumns);

        Stream.of("Time", "Account Activity", "Amount")
                .forEach(title -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.CYAN);
                    header.setBorderWidth(1);
                    header.setPhrase(new Phrase(title));
                    table.addCell(header);
                });

        for (Map<String, Object> summary : summaries) {
            String[] values = new String[numberOfColumns];

            for (Map.Entry<String, Object> entry : summary.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                switch (key) {
                    case SummaryField.TIME -> values[0] = value;
                    case SummaryField.ACCOUNT_ACTIVITY -> values[1] = value;
                    case SummaryField.AMOUNT -> values[2] = value;
                    default -> log.info("Unmatched field. Field: {}", key);
                }
            }

            for (String value : values) {
                table.addCell(value);
            }
        }

        document.add(table);
    }

    private static String maskField(Map.Entry<String, Object> entry) {
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

    private static StringBuilder maskWordInFullName(String word) {
        int length = word.length();
        int endIndex = length < 5 ? 1 : 2;
        log.info("Length and end index: {} & {}", length, endIndex);

        return new StringBuilder()
                .append(word, 0, endIndex)
                .append("*".repeat(length - endIndex));
    }

    private static void addBottom(Document document) throws DocumentException, IOException {
        Paragraph paragraph = new Paragraph("Thank you for working with us");
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        addLogo(document);

        Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.GREEN);
        String message = "Online Bank";

        paragraph = new Paragraph(message, boldFont);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
    }

    private static void addLogo(Document document) throws DocumentException, IOException {
        Path path = Paths.get("/app/photo/logo.png");
        log.info("Path: {}", path);

        Image image = Image.getInstance(path.toAbsolutePath().toString());
        image.scalePercent(10);
        image.setAlignment(Element.ALIGN_CENTER);

        document.add(image);
    }
}
