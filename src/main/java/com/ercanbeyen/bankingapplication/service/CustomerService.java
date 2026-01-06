package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.response.CustomerFinancialStatusResponse;
import com.ercanbeyen.bankingapplication.embeddable.ExpectedTransaction;
import com.ercanbeyen.bankingapplication.embeddable.RegisteredRecipient;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.option.CustomerFilteringOption;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CustomerService extends BaseService<CustomerDto, CustomerFilteringOption> {
    String approveAgreement(Integer id, String title);
    String addRegisteredRecipient(Integer id, RegisteredRecipient request);
    String removeRegisteredRecipient(Integer id, Integer recipientAccountId);
    String uploadProfilePhoto(Integer id, MultipartFile request);
    File downloadProfilePhoto(Integer id);
    String deleteProfilePhoto(Integer id);
    CustomerFinancialStatusResponse calculateFinancialStatus(String nationalId, Currency toCurrency);
    List<AccountDto> getAccounts(Integer id, AccountFilteringOption filteringOption);
    List<NotificationDto> getNotifications(Integer id);
    List<MoneyTransferOrderDto> getMoneyTransferOrders(Integer customerId, LocalDate fromDate, LocalDate toDate, Currency currency, PaymentType paymentType);
    CashFlowCalendarDto getCashFlowCalendar(Integer id, Integer year, Integer month);
    List<ExpectedTransaction> getExpectedTransactions(Integer id, Integer month);
    List<CustomerAgreementDto> getAgreements(Integer id);
    List<RegisteredRecipient> getRegisteredRecipients(Integer id);
    Customer findByNationalId(String nationalId);
    boolean existsByNationalId(String nationalId);
}
