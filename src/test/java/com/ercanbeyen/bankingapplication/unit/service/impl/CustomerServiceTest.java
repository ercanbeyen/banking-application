package com.ercanbeyen.bankingapplication.unit.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerAgreementDto;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.entity.*;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.factory.*;
import com.ercanbeyen.bankingapplication.mapper.CustomerAgreementMapper;
import com.ercanbeyen.bankingapplication.mapper.CustomerMapper;
import com.ercanbeyen.bankingapplication.dto.option.CustomerFilteringOption;
import com.ercanbeyen.bankingapplication.repository.CustomerRepository;
import com.ercanbeyen.bankingapplication.service.CashFlowCalendarService;
import com.ercanbeyen.bankingapplication.service.AgreementService;
import com.ercanbeyen.bankingapplication.service.EmailService;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.service.impl.CustomerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Service Test")
class CustomerServiceTest {
    public static final String TESTED_CLASS = "Customer Service";
    @InjectMocks
    private CustomerServiceImpl customerService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private CustomerAgreementMapper customerAgreementMapper;
    @Mock
    private FileService fileService;
    @Mock
    private CashFlowCalendarService cashFlowCalendarService;
    @Mock
    private AgreementService agreementService;
    @Mock
    private EmailService emailService;

    private List<Customer> customers;
    private List<CustomerAgreementDto> customerAgreementDtos;
    private List<CustomerDto> customerDtos;
    private List<CashFlowCalendar> cashFlowCalendars;

    @BeforeAll
    static void start() {
        log.info(LogMessage.Test.START, LogMessage.Test.UNIT, TESTED_CLASS);
    }

    @AfterAll
    static void end() {
        log.info(LogMessage.Test.END, LogMessage.Test.UNIT, TESTED_CLASS);
    }

    @BeforeEach
    void setUp() {
        log.info(LogMessage.Test.SETUP);
        customers = MockCustomerFactory.generateMockCustomers();
        customerDtos = MockCustomerFactory.generateMockCustomerDtos();
        cashFlowCalendars = MockCashFlowCalendarFactory.generateMockCashFlowCalendars();
        customerAgreementDtos = MockCustomerAgreementFactory.generateMockCustomerAgreementDtos();
    }

    @AfterEach
    void tearDown() {
        log.info(LogMessage.Test.TEAR_DOWN);
    }

    @Test
    @DisplayName("Happy path test: Given filtering option when getEntities then return CustomerDtos")
    void givenFilteringOption_whenGetEntities_thenReturnCustomerDtos() {
        // given
        List<CustomerDto> expected = List.of(customerDtos.getFirst());
        CustomerFilteringOption filteringOption = new CustomerFilteringOption();
        filteringOption.setBirthDate(LocalDate.of(1980, 8, 15));

        doReturn(customers)
                .when(customerRepository)
                .findAll();
        doReturn(expected.getFirst())
                .when(customerMapper)
                .entityToDto(any());

        // when
        List<CustomerDto> actual = customerService.getEntities(filteringOption);

        // then
        verify(customerRepository, times(1)).findAll();
        verify(customerMapper, times(1)).entityToDto(any());

        assertEquals(expected.size(), actual.size());
    }

    @Test
    @DisplayName("Happy path test: Given existing id when getEntity then return CustomerDto")
    void givenExistingId_whenGetEntity_thenReturnCustomerDto() {
        // given
        Optional<CustomerDto> expected = Optional.of(customerDtos.getFirst());
        Customer customer = customers.getFirst();

        doReturn(Optional.of(customer))
                .when(customerRepository)
                .findById(anyInt());
        doReturn(expected.get())
                .when(customerMapper)
                .entityToDto(any());

        // when
        CustomerDto actual = customerService.getEntity(customer.getId());

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verify(customerMapper, times(1)).entityToDto(any());

        assertEquals(expected.get().getId(), actual.getId());
    }

    @Test
    @DisplayName("Exception path test: Given not existing id when getEntity then throw ResourceNotFoundException")
    void givenNotExistingId_whenGetEntity_thenThrowResourceNotFoundException() {
        // given
        String expected = String.format(ResponseMessage.NOT_FOUND, Entity.CUSTOMER.getValue());

        doReturn(Optional.empty())
                .when(customerRepository)
                .findById(anyInt());

        // when
        RuntimeException exception = assertThrows(ResourceNotFoundException.class, () -> customerService.getEntity(20));
        String actual = exception.getMessage();

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verifyNoMoreInteractions(customerRepository, customerMapper);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Happy path test: Given CustomerDto when createEntity then return CustomerDto")
    void givenCustomerDto_whenCreateEntity_thenReturnCustomerDto() {
        // given
        Customer customer = customers.getFirst();
        CustomerDto expected = customerDtos.getFirst();
        CustomerDto request = customerDtos.getFirst();
        CashFlowCalendar cashFlowCalendar = cashFlowCalendars.getFirst();

        doReturn(customer)
                .when(customerMapper)
                .dtoToEntity(any());
        doReturn(cashFlowCalendar)
                .when(cashFlowCalendarService)
                .createCashFlowCalendar();
        doReturn(customer)
                .when(customerRepository)
                .save(any());
        doNothing()
                .when(agreementService)
                .approveAgreements(any(), any());
        doReturn(expected)
                .when(customerMapper)
                .entityToDto(any());

        // when
        CustomerDto actual = customerService.createEntity(request);

        // then
        verify(customerRepository, times(1)).findAll();
        verify(customerMapper, times(1)).dtoToEntity(any());
        verify(cashFlowCalendarService, times(1)).createCashFlowCalendar();
        verify(customerRepository, times(1)).save(any());
        verify(agreementService, times(1)).approveAgreements(any(), any());
        verify(customerMapper, times(1)).entityToDto(any());

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Exception path test: Given CustomerDto when createEntity then throw ResourceConflictException")
    void givenCustomerDto_whenCreateEntity_thenThrowResourceConflictException() {
        // given
        CustomerDto request = MockCustomerFactory.generateMockCustomerDtos().getFirst();
        String expected = String.format(ResponseMessage.ALREADY_EXISTS, Entity.CUSTOMER.getValue());

        doReturn(customers).when(customerRepository).findAll();

        // when
        RuntimeException exception = assertThrows(ResourceConflictException.class, () -> customerService.createEntity(request));
        String actual = exception.getMessage();

        // then
        verify(customerRepository, times(1)).findAll();
        verifyNoMoreInteractions(customerRepository, customerMapper);

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+905328465704", "+905328465705"})
    @DisplayName("Happy path test: Given phone number when updateEntity then return CustomerDto")
    void givenPhoneNumber_whenUpdateEntity_thenReturnCustomerDto(String phoneNumber) {
        // given
        CustomerDto request = customerDtos.getFirst();
        request.setPhoneNumber(phoneNumber);

        Customer customer = customers.getFirst();

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());
        doReturn(customer)
                .when(customerRepository)
                .save(any());
        doReturn(request)
                .when(customerMapper)
                .entityToDto(any());

        // when
        CustomerDto actual = customerService.updateEntity(customer.getId(), request);

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verify(customerRepository, times(1)).save(any());
        verify(customerMapper, times(1)).entityToDto(any());
        verifyNoInteractions(emailService);

        assertEquals(phoneNumber, actual.getPhoneNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+905328465702", "+905328465703"})
    @DisplayName("Exception path test: Given phone number when updateEntity then throw ResourceConflictException")
    void givenPhoneNumber_whenUpdateEntity_thenThrowResourceConflictException(String phoneNumber) {
        // given
        CustomerDto request = customerDtos.getFirst();
        request.setPhoneNumber(phoneNumber);
        String expected = String.format(ResponseMessage.ALREADY_EXISTS, Entity.CUSTOMER.getValue());

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());
        doReturn(customers)
                .when(customerRepository)
                .findAll();

        // when
        RuntimeException exception = assertThrows(ResourceConflictException.class, () -> customerService.updateEntity(1, request));
        String actual = exception.getMessage();

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verify(customerRepository, times(1)).findAll();
        verifyNoInteractions(emailService);
        verifyNoMoreInteractions(customerRepository, customerMapper);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Happy path test: Given email when updateEntity then return CustomerDto")
    void givenEmail_whenUpdateEntity_thenReturnCustomerDto() {
        // given
        String email = "updatedTest@email.com";
        CustomerDto request = customerDtos.getFirst();
        request.setEmail(email);

        Customer customer = customers.getFirst();

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());
        doReturn(customer)
                .when(customerRepository)
                .save(any());
        doNothing()
                .when(emailService)
                .sendEmail(any(), any(), any());
        doReturn(request)
                .when(customerMapper)
                .entityToDto(any());

        // when
        CustomerDto actual = customerService.updateEntity(customer.getId(), request);

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verify(customerRepository, times(1)).save(any());
        verify(emailService, times(1)).sendEmail(any(), any(), any());
        verify(customerMapper, times(1)).entityToDto(any());

        assertEquals(email, actual.getEmail());
    }

    @Test
    @Timeout(value = 5) // The default time unit is seconds
    @DisplayName("Happy path test: Given file when uploadPhoto then return message")
    void givenMultipartFile_whenUploadPhoto_thenReturnMessage() throws IOException {
        // given
        MultipartFile multipartFile = MockFileFactory.generateMockMultipartFile();
        File file = MockFileFactory.generateMockFile();
        CompletableFuture<File> fileCompletableFuture = CompletableFuture.supplyAsync(() -> file);

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(customers.getFirst().getId());
        doReturn(fileCompletableFuture)
                .when(fileService)
                .storeFile(any(), any());
        doReturn(customers.getFirst())
                .when(customerRepository)
                .save(any());

        // when
        customerService.uploadProfilePhoto(customers.getFirst().getId(), multipartFile);

        // then
        verify(customerRepository, times(1))
                .findById(anyInt());
        verify(fileService, times(1)).storeFile(any(), any());
    }

    @Test
    @DisplayName("Exception path test: given multipartFile when uploadFile then throw ResourceExpectationFailedException")
    void givenMultipartFile_whenUploadFile_thenThrowResourceExpectationFailedException() {
        // given
        String expected = ResponseMessage.FILE_UPLOAD_ERROR;
        int id = 1;

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());
        doThrow(new ResourceExpectationFailedException(ResponseMessage.FILE_UPLOAD_ERROR))
                .when(fileService)
                .storeFile(any(), any());

        // when
        RuntimeException exception = assertThrows(ResourceExpectationFailedException.class, () -> customerService.uploadProfilePhoto(id, null));
        String actual = exception.getMessage();

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verify(fileService, times(1)).storeFile(any(), any());
        verifyNoMoreInteractions(customerRepository);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Happy path test: Given id when downloadProfilePhoto then return file")
    void givenId_whenDownloadProfilePhoto_thenReturnFile() throws IOException {
        // given
        File expected = MockFileFactory.generateMockFile();
        int id = customers.getFirst().getId();
        customers.getFirst().setProfilePhoto(expected);

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());

        // when
        File actual = customerService.downloadProfilePhoto(id);

        // then
        verify(customerRepository, times(1)).findById(anyInt());

        assertEquals(expected.getName(), actual.getName());
    }

    @Test
    @DisplayName("Exception path test: Given id when downloadProfilePhoto then throw ResourceNotFoundException")
    void givenId_whenDownloadProfilePhoto_thenThrowResourceNotFoundException() {
        // given
        String expected = String.format(ResponseMessage.NOT_FOUND, Entity.CUSTOMER.getValue());

        doReturn(Optional.empty())
                .when(customerRepository)
                .findById(anyInt());

        // when
        RuntimeException exception = assertThrows(ResourceNotFoundException.class, () -> customerService.downloadProfilePhoto(20));
        String actual = exception.getMessage();

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verifyNoMoreInteractions(fileService);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Happy path test: Given id when deletePhoto then return message")
    void givenId_whenDeletePhoto_thenReturnMessage() {
        // given
        int id = customers.getFirst().getId();

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());

        // when
        customerService.deleteProfilePhoto(id);

        // then
        verify(customerRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Happy path test: Given id when getAgreements then return CustomerAgreements")
    void givenId_whenGetAgreements_thenReturnCustomerAgreements() {
        // given
        int id = customers.getFirst().getId();

        List<CustomerAgreementDto> expected = customerAgreementDtos;

        doReturn(Optional.of(customers.getFirst()))
                .when(customerRepository)
                .findById(anyInt());
        doReturn(expected.getFirst())
                .when(customerAgreementMapper)
                .entityToDto(any());

        // when
        List<CustomerAgreementDto> actual = customerService.getAgreements(id);

        // then
        verify(customerRepository, times(1)).findById(anyInt());
        verify(customerAgreementMapper, times(1)).entityToDto(any());

        assertEquals(expected.size(), actual.size());
    }
}
