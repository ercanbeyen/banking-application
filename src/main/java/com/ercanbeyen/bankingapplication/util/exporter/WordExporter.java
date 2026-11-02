package com.ercanbeyen.bankingapplication.util.exporter;

import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.util.AccountStatementUtil;
import com.ercanbeyen.bankingapplication.util.ExporterUtil;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import lombok.experimental.UtilityClass;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@UtilityClass
public class WordExporter {
    public XWPFDocument generateAccountStatementWord(Account account, ZoneId zoneId, LocalDate fromDate, LocalDate toDate, List<AccountActivityPreview> accountActivityPreviews) throws IOException, InvalidFormatException {
        XWPFDocument document = new XWPFDocument();

        writeHeader(document);
        writeTitle(document);
        addNewLine(document);

        writeAccountStatementBody(document, account, zoneId, fromDate, toDate, accountActivityPreviews);
        addNewLine(document);

        writerFooter(document);

        return document;
    }

    private void writeAccountStatementBody(XWPFDocument document, Account account, ZoneId zoneId, LocalDate fromDate, LocalDate toDate, List<AccountActivityPreview> accountActivityPreviews) {
        writeInformationTable(document, account, zoneId, fromDate, toDate);
        addNewLine(document);
        writeAccountActivityTable(document, accountActivityPreviews);
    }

    private void writeInformationTable(XWPFDocument document, Account account, ZoneId zoneId, LocalDate fromDate, LocalDate toDate) {
        XWPFTable mainTable = document.createTable(1, 2);
        mainTable.setTableAlignment(TableRowAlign.CENTER);

        XWPFTableCell accountInformationCell = mainTable.getRow(0).getCell(0);
        XWPFTable accountInformationTable = accountInformationCell.insertNewTbl(accountInformationCell.getParagraphArray(0).getCTP().newCursor());
        accountInformationTable.setTableAlignment(TableRowAlign.CENTER);

        Customer customer = account.getCustomer();
        Map.Entry<String, Object> entry = Map.entry(SummaryField.NATIONAL_IDENTITY, customer.getNationalId());


        XWPFTableRow row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.writeFullName(customer.getName()));

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.CUSTOMER_NUMBER + customer.getId());

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.CUSTOMER_NATIONAL_IDENTITY_NUMBER + ExporterUtil.maskField(entry));

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.BRANCH + account.getBranch().getName());

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.ACCOUNT_IDENTITY + account.getId());

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.ACCOUNT_TYPE + account.getType());

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.ACCOUNT_CURRENCY + account.getCurrency());

        row = accountInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.BALANCE + FormatterUtil.convertNumberToFormalExpression(account.getBalance()));

        XWPFTableCell transactionInformationCell = mainTable.getRow(0).getCell(1);
        XWPFTable transactionInformationTable = transactionInformationCell.insertNewTbl(transactionInformationCell.getParagraphArray(0).getCTP().newCursor());
        transactionInformationTable.setTableAlignment(TableRowAlign.CENTER);

        row = transactionInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.DOCUMENT_ISSUE_DATE + TimeUtil.getLocalDateTimeStatement(LocalDateTime.now(zoneId)));

        row = transactionInformationTable.createRow();
        row.addNewTableCell().setText(AccountStatementUtil.INQUIRY_CRITERIA + AccountStatementUtil.writeInquiryCriteria(fromDate, toDate));
    }

    private void writeAccountActivityTable(XWPFDocument document, List<AccountActivityPreview> accountActivityPreviews) {
        XWPFTable table = document.createTable();
        writeHeaderRow(table);
        writeDataRows(accountActivityPreviews, table);
    }

    private void writeDataRows(List<AccountActivityPreview> accountActivityPreviews, XWPFTable table) {
        for (AccountActivityPreview accountActivityPreview : accountActivityPreviews) {
            XWPFTableRow row = table.createRow();

            XWPFTableCell cell = row.getCell(0);
            cell.setText(TimeUtil.getLocalDateTimeStatement(accountActivityPreview.createdAt()));

            cell = row.getCell(1);
            cell.setText(accountActivityPreview.activityType().getValue());

            cell = row.getCell(2);
            cell.setText(FormatterUtil.convertNumberToFormalExpression(ExporterUtil.calculateAmountForDataLine(accountActivityPreview)));

            for (XWPFTableCell cellInRow : row.getTableCells()) {
                /* Center horizontally */
                for (XWPFParagraph paragraphInRow : cellInRow.getParagraphs()) {
                    paragraphInRow.setAlignment(ParagraphAlignment.CENTER);
                }

                /* Center vertically */
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }
    }

    private void writeHeaderRow(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();

        CTTblCellMar cellMar = tblPr.isSetTblCellMar()
                ? tblPr.getTblCellMar()
                : tblPr.addNewTblCellMar();

        CTTblWidth topWidth = cellMar.addNewTop();
        topWidth.setW(BigInteger.valueOf(100));
        topWidth.setType(STTblWidth.DXA);

        CTTblWidth bottomWidth = cellMar.addNewBottom();
        bottomWidth.setW(BigInteger.valueOf(100));
        bottomWidth.setType(STTblWidth.DXA);

        CTTblWidth leftWidth = cellMar.addNewLeft();
        leftWidth.setW(BigInteger.valueOf(120));
        leftWidth.setType(STTblWidth.DXA);

        CTTblWidth rightWidth = cellMar.addNewRight();
        rightWidth.setW(BigInteger.valueOf(120));
        rightWidth.setType(STTblWidth.DXA);

        CTJcTable jcTable = tblPr.isSetJc()
                ? tblPr.getJc()
                : tblPr.addNewJc();

        jcTable.setVal(STJcTable.CENTER);

        XWPFTableRow row = table.getRow(0);

        row.getCell(0).setText(SummaryField.TIME);
        row.addNewTableCell().setText(SummaryField.ACCOUNT_ACTIVITY);
        row.addNewTableCell().setText(SummaryField.AMOUNT);

        CTTrPr trPr = row.getCtRow().isSetTrPr()
                ? row.getCtRow().getTrPr()
                : row.getCtRow().addNewTrPr();

        trPr.addNewTblHeader().setVal(true);

        for (XWPFTableCell cellInRow : row.getTableCells()) {
            CTTcPr tcPr = cellInRow.getCTTc().isSetTcPr()
                    ? cellInRow.getCTTc().getTcPr()
                    : cellInRow.getCTTc().addNewTcPr();

            CTShd shd = tcPr.isSetShd()
                    ? tcPr.getShd()
                    : tcPr.addNewShd();

            shd.setFill("0E69E8");
            shd.setVal(STShd.CLEAR);

            cellInRow.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            for (XWPFParagraph paragraphInCell : cellInRow.getParagraphs()) {
                paragraphInCell.setAlignment(ParagraphAlignment.CENTER);

                for (XWPFRun run : paragraphInCell.getRuns()) {
                    run.setColor("FFFFFF");
                    run.setBold(true);
                    run.setFontSize(12);
                }
            }
        }
    }

    private void writeTitle(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun paragraphRun = paragraph.createRun();
        paragraphRun.setText(ExporterUtil.getAccountStatementTitle());
        paragraphRun.setColor("E8280E");
        paragraphRun.setBold(true);
        paragraphRun.setFontFamily("Helvetica");
        paragraphRun.setFontSize(20);
    }

    private void writeHeader(XWPFDocument document) throws IOException, InvalidFormatException {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun paragraphRun = paragraph.createRun();
        paragraphRun.setText(ExporterUtil.getBankName());
        paragraphRun.setColor("0E69E8");
        paragraphRun.setBold(true);
        paragraphRun.setFontFamily("Helvetica");
        paragraphRun.setFontSize(16);

        writeLogo(document);
    }

    private void writerFooter(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun paragraphRun = paragraph.createRun();
        paragraphRun.setText(ExporterUtil.getLawMessage());
        paragraphRun.setFontSize(8);
        paragraphRun.setItalic(true);

        paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        paragraphRun = paragraph.createRun();
        paragraphRun.setText(ExporterUtil.getTimeZoneMessage());
        paragraphRun.setFontSize(8);
        paragraphRun.setItalic(true);
    }

    private void writeLogo(XWPFDocument document) throws IOException, InvalidFormatException {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun paragraphRun = paragraph.createRun();
        paragraphRun.setTextPosition(20);

        Path imagePath = Paths.get(ExporterUtil.getLogoPath()).toAbsolutePath();
        paragraphRun.addPicture(Files.newInputStream(imagePath),
                XWPFDocument.PICTURE_TYPE_PNG, imagePath.getFileName().toString(),
                Units.toEMU(50), Units.toEMU(50));
    }

    private void addNewLine(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun paragraphRun = paragraph.createRun();
        paragraphRun.setText("\n");
    }
}
