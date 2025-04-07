package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class AccountActivityUtil {
    private static final List<String> customerCredentials = List.of(SummaryField.FULL_NAME, SummaryField.NATIONAL_IDENTITY);

    public ByteArrayOutputStream generatePdfStream(Map<String, Object> summary) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        addTitle(document);
        document.add(new Paragraph("\n"));

        addTable(document, summary);
        document.add(new Paragraph("\n"));

        addBottom(document);
        document.close();

        return outputStream;
    }

    private static void addTitle(Document document) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, BaseColor.RED);
        String message = "RECEIPT";

        Paragraph paragraph = new Paragraph(message, boldFont);
        paragraph.setAlignment(Element.ALIGN_CENTER);

        document.add(paragraph);
    }

    private static void addTable(Document document, Map<String, Object> summary) throws DocumentException {
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
