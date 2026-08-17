package com.ercanbeyen.bankingapplication.util.exporter;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Channel;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.util.AccountStatementUtil;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountFinancialStatus;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.helper.event.BorderEvent;
import com.ercanbeyen.bankingapplication.helper.event.PageNumerationEvent;
import com.ercanbeyen.bankingapplication.util.ExporterUtil;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@UtilityClass
public class PdfExporter {
    private final List<String> maskedSummaryFields = List.of(SummaryField.FULL_NAME, SummaryField.NATIONAL_IDENTITY);

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

        String todayDateTime = AccountStatementUtil.writeDocumentIssueDate(LocalDateTime.now(ZoneId.systemDefault()));

        Chunk dateOutputChunk = new Chunk(todayDateTime);

        Phrase chunkPhrase = new Phrase();
        chunkPhrase.addAll(List.of(fullNameInputChunk, fullNameOutputChunk, dateInputChunk, dateOutputChunk));

        Paragraph paragraph = new Paragraph(chunkPhrase);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        addNewLine(document);

        PdfPTable table = new PdfPTable(2);

        writeHeaderRowOfTable.accept(List.of("Asset", "Balance"), table);

        Font font = new Font(Font.FontFamily.HELVETICA, Font.DEFAULTSIZE, Font.BOLD);
        final Currency financialStatusReportCurrency = Currency.getDeductionCurrency();

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
        Map<String, Object> receiptSummary = generateReceiptSummary(accountActivity);

        for (Map.Entry<String, Object> entry : receiptSummary.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();

            for (String maskedSummaryField : maskedSummaryFields) {
                if (key.contains(maskedSummaryField)) {
                    value = maskField(entry);
                }
            }

            if (key.contains(SummaryField.TIME)) {
                LocalDateTime dateTime = LocalDateTime.parse(receiptSummary.get(key).toString());
                value = AccountStatementUtil.writeDocumentIssueDate(dateTime);
            }

            table.addCell(key);
            table.addCell(value);
        }

        document.add(table);
    }

    private Map<String, Object> generateReceiptSummary(AccountActivity accountActivity) {
        String customerNationalId = ((UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal())
                .getUsername();

        Map<String, Object> summary = accountActivity.getSummary();
        AccountActivityType accountActivityType = accountActivity.getType();
        List<AccountActivityType> filteredAccountActivityTypes = List.of(AccountActivityType.MONEY_TRANSFER, AccountActivityType.MONEY_EXCHANGE, AccountActivityType.DEDUCTION);

        if (!filteredAccountActivityTypes.contains(accountActivityType)) {
            return summary;
        }

        Map<String, Object> receiptSummary = new HashMap<>(summary);
        List<String> accountPositions = new ArrayList<>(List.of("Sender", "Recipient"));

        switch (accountActivityType) {
            case AccountActivityType.MONEY_TRANSFER -> {
                String accountPosition = getAccountPositionFromMoneyTransfer(accountActivity, accountPositions, customerNationalId, receiptSummary);
                accountPositions.remove(accountPosition);
                String accountPositionRemoved = accountPositions.getFirst();
                accountPositions.clear();

                removeDeducteeInformation(receiptSummary);

                for (Map.Entry<String, Object> entry : summary.entrySet()) {
                    String key = entry.getKey();

                    if (key.contains(SummaryField.FULL_NAME) || key.contains(SummaryField.ACCOUNT_IDENTITY)) {
                        continue;
                    }

                    if (key.contains(accountPositionRemoved)) {
                        receiptSummary.remove(key);
                    }
                }
            }
            case AccountActivityType.MONEY_EXCHANGE -> removeDeducteeInformation(receiptSummary);
            default -> { // Deduction case
                String accountActivityInSummary = summary.get(SummaryField.ACCOUNT_ACTIVITY).toString();

                if (accountActivityInSummary.equals(AccountActivityType.MONEY_TRANSFER.getValue())) {
                    for (Map.Entry<String, Object> entry : summary.entrySet()) {
                        String key = entry.getKey();

                        for (String currentAccountPosition : accountPositions) {
                            if (key.contains(currentAccountPosition)) {
                                receiptSummary.remove(key);
                            }
                        }
                    }

                    String fullName = accountActivity.getSenderAccount()
                            .getCustomer()
                            .getFullName();

                    receiptSummary.put(SummaryField.FULL_NAME, fullName);
                    receiptSummary.remove(SummaryField.PAYMENT_TYPE);
                } else if (accountActivityInSummary.equals(AccountActivityType.MONEY_EXCHANGE.getValue())) {
                    receiptSummary.remove("Spent " + SummaryField.AMOUNT);
                    receiptSummary.remove("Earned " + SummaryField.AMOUNT);
                    receiptSummary.remove(SummaryField.RATE);

                    String sellerWord = "Seller ";
                    String buyerWord = "Buyer ";

                    receiptSummary.remove(sellerWord + SummaryField.ACCOUNT_IDENTITY);
                    receiptSummary.remove(buyerWord + SummaryField.ACCOUNT_IDENTITY);
                    receiptSummary.remove(sellerWord + SummaryField.TIME);
                    receiptSummary.remove(buyerWord + SummaryField.TIME);
                }

                receiptSummary.put(SummaryField.ACCOUNT_ACTIVITY, AccountActivityType.DEDUCTION.getValue());
                receiptSummary.put(SummaryField.CHANNEL, accountActivity.getChannel());
                receiptSummary.remove(SummaryField.AMOUNT);
            }
        }

        return receiptSummary;
    }

    private String getAccountPositionFromMoneyTransfer(AccountActivity accountActivity, List<String> accountPositions, String customerNationalId, Map<String, Object> receiptSummary) {
        String senderAccountNationalId = accountActivity.getSenderAccount()
                .getCustomer()
                .getNationalId();
        String recipientAccountNationalId = accountActivity.getRecipientAccount()
                .getCustomer()
                .getNationalId();

        if (senderAccountNationalId.equals(recipientAccountNationalId)) { // accounts of same customer
            return accountPositions.getFirst();
        }

        /* Accounts of different customers */
        String accountPosition;

        if (senderAccountNationalId.equals(customerNationalId)) {
            accountPosition = accountPositions.getFirst();
        } else { // customer's account is recipient
            accountPosition = accountPositions.getLast();
            receiptSummary.remove(SummaryField.TRANSACTION_FEE);
            receiptSummary.put(SummaryField.CHANNEL, Channel.AUTOMATIC);
        }

        return accountPosition;
    }

    private void removeDeducteeInformation(Map<String, Object> receiptSummary) {
        final String deducteeWord = "Deductee ";
        receiptSummary.remove(deducteeWord + SummaryField.TIME);
        receiptSummary.remove(deducteeWord + SummaryField.ACCOUNT_IDENTITY);
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

        transactionInformationTable.addCell(AccountStatementUtil.DOCUMENT_ISSUE_DATE + AccountStatementUtil.writeDocumentIssueDate(LocalDateTime.now(ZoneId.systemDefault())));
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

            return new StringBuilder()
                    .append(word, 0, endIndex)
                    .append("*".repeat(length - endIndex));
        };

        if (key.contains(SummaryField.FULL_NAME)) {
            int spaceIndex = value.indexOf(' ');
            String name = value.substring(0, spaceIndex);
            String surname = value.substring(spaceIndex + 1);

            valueBuilder.append(maskWordInFullName.apply(name))
                    .append(" ")
                    .append(maskWordInFullName.apply(surname));
        } else if (key.contains(SummaryField.NATIONAL_IDENTITY)) {
            int length = value.length();
            valueBuilder.append(value, 0, 3)
                    .append("*".repeat(length - 5))
                    .append(value, length - 2, length);
        } else {
            throw new ResourceConflictException(String.format("Summary field %s is not in %s", key, maskedSummaryFields));
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
