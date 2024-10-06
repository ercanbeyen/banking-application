package com.ercanbeyen.bankingapplication.util;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@UtilityClass
public class AccountActivityUtils {

    public ByteArrayOutputStream generatePdfStream(Map<String, Object> summary) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        for (Map.Entry<String, Object> entry : summary.entrySet()) {
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            String field = entry.getKey() + " " + entry.getValue();
            Paragraph paragraph = new Paragraph(field, boldFont);
            document.add(paragraph);
        }

        document.add(new Paragraph("\n"));
        document.close();

        return outputStream;
    }
}
