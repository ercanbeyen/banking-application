package com.ercanbeyen.bankingapplication.exporter;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.util.AccountStatementUtil;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountFinancialStatus;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.event.BorderEvent;
import com.ercanbeyen.bankingapplication.event.PageNumerationEvent;
import com.ercanbeyen.bankingapplication.util.ExporterUtil;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
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
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@UtilityClass
public class PdfExporter {
    private final List<String> customerCredentials = List.of(SummaryField.FULL_NAME, SummaryField.NATIONAL_IDENTITY);

    public ByteArrayOutputStream generatePdfStreamOfFinancialStatusReport(Customer customer, Double netBalanceOfCustomer, Map<AccountType, Double> netBalancesOfAccountTypes, Map<AccountType, List<List<AccountFinancialStatus>>> financialStatusesOfAccountTypesWithConvertedCurrencies) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPageEvent(new PageNumerationEvent());
        document.open();

        writeHeader(document);
        writeTitle(document, "financial status");
        addNewLine(document);

        writeFinancialStatusReportBody(document, customer, netBalanceOfCustomer, netBalancesOfAccountTypes, financialStatusesOfAccountTypesWithConvertedCurrencies);
        addNewLine(document);

        writeFinancialStatusReportFooter(document);
        document.close();

        return outputStream;
    }

    public ByteArrayOutputStream generatePdfStreamOfReceipt(AccountActivity accountActivity) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        writeHeader(document);
        writeTitle(document, "receipt");
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

        Paragraph paragraph = new Paragraph(title.toUpperCase(), font);
        paragraph.setAlignment(Element.ALIGN_CENTER);

        document.add(paragraph);
    }

    private void writeFinancialStatusReportBody(Document document, Customer customer, Double netBalanceOfCustomer, Map<AccountType, Double> netBalancesOfAccountTypes, Map<AccountType, List<List<AccountFinancialStatus>>> financialStatusesOfAccountTypesWithConvertedCurrencies) throws DocumentException {
        final Font boldFont = new Font();
        boldFont.setStyle(Font.BOLD);

        Chunk fullNameInputChunk = new Chunk(SummaryField.FULL_NAME + ": ", boldFont);
        Chunk fullNameOutputChunk = new Chunk(customer.getFullName());

        Chunk dateInputChunk = new Chunk("  Date: ", boldFont);
        LocalDateTime today = TimeUtil.getTurkeyDateTime();
        String todayDate = today.toLocalDate().toString();
        String todayTime = TimeUtil.getTimeStatement(today.toLocalTime());

        Chunk dateOutputChunk = new Chunk(todayDate + " " + todayTime);

        Phrase chunkPhrase = new Phrase();
        chunkPhrase.addAll(List.of(fullNameInputChunk, fullNameOutputChunk, dateInputChunk, dateOutputChunk));

        Paragraph paragraph = new Paragraph(chunkPhrase);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        addNewLine(document);

        PdfPTable table = new PdfPTable(2);

        writeHeaderRowOfTable.accept(List.of("Asset", "Balance"), table);

        Font font = new Font(Font.FontFamily.HELVETICA, Font.DEFAULTSIZE, Font.BOLD);
        final Currency financialStatusReportCurrency = Currency.getChargeCurrency();

        for (Map.Entry<AccountType, List<List<AccountFinancialStatus>>> financialStatusOfAccountType : financialStatusesOfAccountTypesWithConvertedCurrencies.entrySet()) {
            /* Header row */
            AccountType accountType = financialStatusOfAccountType.getKey();
            List.of(accountType.getValue(), FormatterUtil.convertNumberToFormalExpression(netBalancesOfAccountTypes.get(accountType)) + " " + financialStatusReportCurrency)
                    .forEach(header -> {
                        PdfPCell cell = new PdfPCell();
                        cell.setBorderWidth(1);
                        Phrase phrase = new Phrase(header, font);
                        cell.setPhrase(phrase);
                        table.addCell(cell);
                    });

            /* Data rows */
            for (List<AccountFinancialStatus> accountFinancialStatuses : financialStatusOfAccountType.getValue()) {
                AccountFinancialStatus accountFinancialStatus = accountFinancialStatuses.getFirst();
                AccountFinancialStatus accountFinancialStatusWithConvertedCurrency = accountFinancialStatuses.getLast();

                StringBuilder stringBuilder = new StringBuilder()
                        .append(FormatterUtil.convertNumberToFormalExpression(accountFinancialStatus.balance()))
                        .append(" ")
                        .append(accountFinancialStatus.currency());

                if (accountFinancialStatus.currency() != accountFinancialStatusWithConvertedCurrency.currency()) {
                    stringBuilder.append(" / ")
                            .append(FormatterUtil.convertNumberToFormalExpression(accountFinancialStatusWithConvertedCurrency.balance()))
                            .append(" ")
                            .append(accountFinancialStatusWithConvertedCurrency.currency());
                }

                table.addCell(new PdfPCell(new Phrase(accountFinancialStatus.currency().toString())));
                table.addCell(new PdfPCell(new Phrase(stringBuilder.toString())));
            }
        }

        font.setSize(13);
        List.of("Sum", FormatterUtil.convertNumberToFormalExpression(netBalanceOfCustomer) + " " + financialStatusReportCurrency)
                .forEach(footer -> {
                    PdfPCell cell = new PdfPCell();
                    cell.setBorderWidth(1);
                    cell.setBackgroundColor(BaseColor.GRAY);
                    Phrase phrase = new Phrase(footer, font);
                    cell.setPhrase(phrase);
                    table.addCell(cell);
                });

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
        writeHeaderRowOfTable.accept(List.of("Field", "Value"), table);

        /* Data rows */
        Map<String, Object> summary = accountActivity.getSummary();

        LocalDateTime localDateTime = LocalDateTime.parse(summary.get(SummaryField.TIME).toString());
        String timeValue = localDateTime.toLocalDate() + " " + TimeUtil.getTimeStatement(localDateTime.toLocalTime());
        summary.put(SummaryField.TIME, timeValue);

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

        accountInformationTable.addCell(AccountStatementUtil.writeFullName(customer.getName()));
        accountInformationTable.addCell(new Phrase(new Paragraph("\n")));
        accountInformationTable.addCell(AccountStatementUtil.CUSTOMER_NUMBER + customer.getId());
        accountInformationTable.addCell(AccountStatementUtil.CUSTOMER_NATIONAL_IDENTITY_NUMBER + maskField(entry));
        accountInformationTable.addCell(AccountStatementUtil.BRANCH + account.getBranch().getName());
        accountInformationTable.addCell(AccountStatementUtil.ACCOUNT_IDENTITY + account.getId());
        accountInformationTable.addCell(AccountStatementUtil.ACCOUNT_TYPE + account.getType());
        accountInformationTable.addCell(AccountStatementUtil.ACCOUNT_CURRENCY + account.getCurrency());
        accountInformationTable.addCell(AccountStatementUtil.BALANCE + FormatterUtil.convertNumberToFormalExpression(account.getBalance()));

        table.addCell(accountInformationTable);

        PdfPTable transactionInformationTable = new PdfPTable(1);
        transactionInformationTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        transactionInformationTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        transactionInformationTable.setTableEvent(borderEvent);

        transactionInformationTable.addCell(AccountStatementUtil.DOCUMENT_ISSUE_DATE + AccountStatementUtil.writeDocumentIssueDate(TimeUtil.getTurkeyDateTime()));
        transactionInformationTable.addCell(AccountStatementUtil.INQUIRY_CRITERIA + AccountStatementUtil.writeInquiryCriteria(fromDate, toDate));

        table.addCell(transactionInformationTable);

        document.add(table);
    }

    private void writeAccountActivityTable(Account account, Document document, List<AccountActivityDto> accountActivityDtos) throws DocumentException {
        final int numberOfColumns = 3;
        PdfPTable table = new PdfPTable(numberOfColumns);

        /* Header row */
        writeHeaderRowOfTable.accept(List.of(SummaryField.TIME, SummaryField.ACCOUNT_ACTIVITY, SummaryField.AMOUNT), table);

        /* Data rows */
        for (AccountActivityDto accountActivityDto : accountActivityDtos) {
            table.addCell(new PdfPCell(new Phrase(accountActivityDto.createdAt().toString())));
            table.addCell(new PdfPCell(new Phrase(accountActivityDto.type().getValue())));
            table.addCell(new PdfPCell(new Phrase(FormatterUtil.convertNumberToFormalExpression(ExporterUtil.calculateAmountForDataLine(account.getId(), accountActivityDto)))));
        }

        document.add(table);
    }

    private final BiConsumer<List<String>, PdfPTable> writeHeaderRowOfTable = (fields, table) -> {
        final Font font = new Font(Font.FontFamily.HELVETICA, Font.DEFAULTSIZE, Font.BOLD, BaseColor.WHITE);
        fields.forEach(field -> {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(BaseColor.BLUE);
            header.setBorderWidth(1);
            Phrase phrase = new Phrase(field, font);
            header.setPhrase(phrase);
            table.addCell(header);
        });
    };

    private String maskField(Map.Entry<String, Object> entry) {
        String key = entry.getKey();
        String value = entry.getValue().toString();
        StringBuilder valueBuilder = new StringBuilder();

        Function<String, StringBuilder> maskWordInFullName = word -> {
            int length = word.length();
            int endIndex = length < 5 ? 1 : 2;
            log.info("Length and end index: {} & {}", length, endIndex);

            return new StringBuilder()
                    .append(word, 0, endIndex)
                    .append("*".repeat(length - endIndex));
        };

        if (key.equals(SummaryField.FULL_NAME)) {
            int spaceIndex = value.indexOf(' ');
            String name = value.substring(0, spaceIndex);
            String surname = value.substring(spaceIndex + 1);

            valueBuilder.append(maskWordInFullName.apply(name))
                    .append(" ")
                    .append(maskWordInFullName.apply(surname));
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

    private void writeFinancialStatusReportFooter(Document document) throws DocumentException {
        Font font = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC);
        String message = "Our immediate banking exchange rates were used in calculating the equivalents for foreign currency assets.";
        Paragraph paragraph = new Paragraph(message, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        writeFooter(document);
    }

    private void writeHeader(Document document) throws DocumentException, IOException {
        Font font = new Font(Font.FontFamily.HELVETICA, Font.DEFAULTSIZE, Font.BOLD, BaseColor.BLUE);

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
