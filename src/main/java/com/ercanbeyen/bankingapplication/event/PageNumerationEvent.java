package com.ercanbeyen.bankingapplication.event;

import com.ercanbeyen.bankingapplication.util.TimeUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PageNumerationEvent extends PdfPageEventHelper {
    PdfTemplate pdfTemplate;
    private final Font normalFont;
    private final Font smallFont;

    public PageNumerationEvent() {
        this.normalFont = new Font(Font.FontFamily.HELVETICA, Font.STRIKETHRU);
        this.smallFont = new Font(Font.FontFamily.TIMES_ROMAN, Font.UNDERLINE);
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        pdfTemplate = writer.getDirectContent().createTemplate(30, 12);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfPTable table = new PdfPTable(3);

        try {
            table.setWidths(new int[]{24, 24, 2});
            table.getDefaultCell().setFixedHeight(10);
            table.getDefaultCell().setBorder(Rectangle.TOP);
            PdfPCell cell = new PdfPCell();
            cell.setBorder(0);
            cell.setBorderWidthTop(1);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setPhrase(new Phrase(TimeUtil.getTurkeyDateTime().toString(), smallFont));
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setBorder(0);
            cell.setBorderWidthTop(1);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPhrase(new Phrase(String.format("Page %d of", writer.getPageNumber()), normalFont));
            table.addCell(cell);

            cell = new PdfPCell(Image.getInstance(pdfTemplate));
            cell.setBorder(0);
            cell.setBorderWidthTop(1);
            table.addCell(cell);
            table.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            table.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin() - 15, writer.getDirectContent());
        } catch (DocumentException exception) {
            throw new ExceptionConverter(exception);
        }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        ColumnText.showTextAligned(
                pdfTemplate,
                Element.ALIGN_LEFT,
                new Phrase(String.valueOf(document.getPageNumber()), normalFont),
                2,
                2,
                0
        );
    }
}
