package com.ercanbeyen.bankingapplication.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.stream.Stream;

@UtilityClass
public class AccountActivityUtils {

    public ByteArrayOutputStream generatePdfStream(Map<String, Object> summary) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        PdfPTable table = new PdfPTable(2);
        addHeader(table);
        addRows(table, summary);
        document.add(table);

        Paragraph paragraph = addThankYouNote();
        document.add(paragraph);

        document.add(new Paragraph("\n"));

        document.close();

        return outputStream;
    }

    private static void addHeader(PdfPTable table) {
        Stream.of("Field", "Value")
                .forEach(title -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.CYAN);
                    header.setBorderWidth(1);
                    header.setPhrase(new Phrase(title));
                    table.addCell(header);
                });
    }

    private static void addRows(PdfPTable table, Map<String, Object> summary) {
        for (Map.Entry<String, Object> entry : summary.entrySet()) {
            table.addCell(entry.getKey());
            table.addCell(entry.getValue().toString());
        }
    }

    private static Paragraph addThankYouNote() {
        Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        String field = "Thank you for working with us";
        Paragraph paragraph = new Paragraph(field, boldFont);

        paragraph.setAlignment(Element.ALIGN_CENTER);

        return paragraph;
    }
}
