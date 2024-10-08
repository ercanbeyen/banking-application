package com.ercanbeyen.bankingapplication.util;

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
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class AccountActivityUtils {

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
            table.addCell(entry.getKey());
            table.addCell(entry.getValue().toString());
        }

        document.add(table);
    }

    private static void addBottom(Document document) throws DocumentException, IOException {
        Paragraph paragraph = new Paragraph("Thank you for working with us");
        paragraph.setAlignment(Element.ALIGN_CENTER);

        document.add(paragraph);
        document.add(new Paragraph("\n"));

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
