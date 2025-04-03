package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatusResponse;
import com.ercanbeyen.bankingapplication.embeddable.ExpectedTransaction;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.option.CustomerFilteringOption;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CustomerService extends BaseService<CustomerDto, CustomerFilteringOption> {
    String uploadProfilePhoto(Integer id, MultipartFile request);
    File downloadProfilePhoto(Integer id);
    String deleteProfilePhoto(Integer id);
    CustomerStatusResponse calculateStatus(String nationalId, Currency toCurrency);
    List<AccountDto> getAccounts(Integer id, AccountFilteringOption filteringOption);
    List<AccountActivityDto> getAccountActivities(Integer id, AccountActivityFilteringOption filteringOption);
    List<NotificationDto> getNotifications(Integer id);
    List<TransferOrderDto> getTransferOrders(Integer customerId, LocalDate fromDate, LocalDate toDate, Currency currency, PaymentType paymentType);
    CashFlowCalendarDto getCashFlowCalendar(Integer id, Integer year, Integer month);
    List<ExpectedTransaction> getExpectedTransactions(Integer id, Integer month);
    List<String> getAgreementSubjects(Integer id);
    Customer findByNationalId(String nationalId);
    boolean existsByNationalId(String nationalId);
}
