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

import java.io.IOException;
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
        XWPFTableRow row = table.getRow(0);

        row.getCell(0).setText(SummaryField.TIME);
        row.addNewTableCell().setText(SummaryField.ACCOUNT_ACTIVITY);
        row.addNewTableCell().setText(SummaryField.AMOUNT);

        for (AccountActivityPreview accountActivityPreview : accountActivityPreviews) {
            row = table.createRow();
            row.getCell(0).setText(TimeUtil.getLocalDateTimeStatement(accountActivityPreview.createdAt()));
            row.getCell(1).setText(accountActivityPreview.activityType().getValue());
            row.getCell(2).setText(FormatterUtil.convertNumberToFormalExpression(ExporterUtil.calculateAmountForDataLine(accountActivityPreview)));
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
