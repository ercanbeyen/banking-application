package com.ercanbeyen.bankingapplication.integration.controller;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.factory.MockAgreementFactory;
import com.ercanbeyen.bankingapplication.factory.MockCustomerFactory;
import com.ercanbeyen.bankingapplication.factory.MockFileFactory;
import com.ercanbeyen.bankingapplication.repository.AgreementRepository;
import com.ercanbeyen.bankingapplication.repository.FileRepository;
import com.ercanbeyen.bankingapplication.security.config.SystemAdminProperties;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Customer Controller Integration Test")
class CustomerControllerTest {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:latest");
    @Container
    @ServiceConnection
    private static final CassandraContainer cassandraContainer = new CassandraContainer("cassandra:latest");
    @Container
    @ServiceConnection
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:latest").withExposedPorts(6379);
    @LocalServerPort
    private Integer port;
    @Autowired
    private Gson gson;
    @Autowired
    private AgreementRepository agreementRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private SystemAdminProperties systemAdminProperties;

    private static final String PHOTO_PATH = "C:\\Users\\ercanbeyen\\Photos\\Banking-App\\Source\\Test\\Resources\\";
    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
    private static final String CUSTOMER_COLLECTION_ENDPOINT = "/api/v1/customers";
    private static final String CUSTOMER_RESOURCE_ENDPOINT = CUSTOMER_COLLECTION_ENDPOINT + "/{id}";
    private static final String PROFILE_PHOTO_UPLOAD_ENDPOINT = CUSTOMER_RESOURCE_ENDPOINT + "/photo/upload";
    private static final String PROFILE_PHOTO_DOWNLOAD_ENDPOINT = CUSTOMER_RESOURCE_ENDPOINT + "/photo/download";
    private static final List<String> accessTokens = new ArrayList<>();
    private static String systemAdminAccessToken;

    @DynamicPropertySource
    static void registerMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);

        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void registerCassandraProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.cassandra.contactpoints", () -> cassandraContainer.getHost() + ":" + cassandraContainer.getFirstMappedPort());
        registry.add("spring.data.cassandra.local-datacenter", cassandraContainer::getLocalDatacenter);
        registry.add("spring.data.cassandra.port", cassandraContainer::getFirstMappedPort);
        registry.add("spring.data.cassandra.keyspace-name", () -> "mykeyspace");
        registry.add("spring.data.cassandra.entity-base-package", () -> "com.ercanbeyen.bankingapplication.entity");
        registry.add("spring.data.cassandra.username", cassandraContainer::getUsername);
        registry.add("spring.data.cassandra.password", cassandraContainer::getPassword);

        cassandraContainer.start();
    }

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", redisContainer::getFirstMappedPort);

        redisContainer.start();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @BeforeAll
    static void start() {
        mySQLContainer.start();
        cassandraContainer.start();
    }

    @AfterAll
    static void end() {
        mySQLContainer.stop();
        cassandraContainer.stop();
    }

    @Test
    @Order(1)
    @DisplayName("Happy path test: Get customers case with no request parameter")
    void whenGetEntities_thenReturnCustomerDtos() {
        systemAdminAccessToken = generateAccessTokenFromUsername(systemAdminProperties.getUsername());

        given()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(systemAdminAccessToken))
                .when()
                .get(CUSTOMER_COLLECTION_ENDPOINT)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("size()", is(1));
    }

    @Test
    @Order(2)
    @DisplayName("Happy path test: Register customer case")
    void givenCustomerDto_whenCreateEntity_thenReturnCustomerDto() {
        generateAgreement();

        Set<String> roles = Set.of(ERole.USER.toString());

        CustomerDto customerDto = MockCustomerFactory.generateMockCustomerDtos().getFirst();
        customerDto.setId(null);

        RegistrationRequest request = new RegistrationRequest(customerDto, "124578", roles);
        registerCustomer(request);

        customerDto = MockCustomerFactory.generateMockCustomerDtos().get(1);
        customerDto.setId(null);

        request = new RegistrationRequest(customerDto, "235878", roles);
        registerCustomer(request);

        customerDto = MockCustomerFactory.generateMockCustomerDtos().getLast();
        customerDto.setId(null);

        request = new RegistrationRequest(customerDto, "256893", roles);
        registerCustomer(request);

        generateAccessTokensOfCustomers();
    }

    @Test
    @Order(3)
    @DisplayName("Happy path test: Get customers case with birth date")
    void givenBirthDate_whenGetEntities_thenReturnCustomerDtos() {
        given()
                .queryParam("birthDate", String.valueOf(MockCustomerFactory.generateMockCustomers().getFirst().getBirthDate()))
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(systemAdminAccessToken))
                .when()
                .get(CUSTOMER_COLLECTION_ENDPOINT)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("size()", is(1));
    }

    @Test
    @Order(4)
    @DisplayName("Happy path test: Get customer case")
    void givenId_whenGetEntity_thenReturnCustomerDto() {
        given()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(accessTokens.getFirst()))
                .when()
                .get(CUSTOMER_RESOURCE_ENDPOINT, 2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("nationalId", equalTo(MockCustomerFactory.generateMockCustomers().getFirst().getNationalId()));
    }

    @Test
    @Order(5)
    @DisplayName("Exception path test: Get customer case")
    void givenId_whenGetEntity_thenThrowAccessDeniedException() {
        given()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(accessTokens.getFirst()))
                .when()
                .get(CUSTOMER_RESOURCE_ENDPOINT, 3)
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Order(6)
    @DisplayName("Exception path test: Update customer case")
    void givenIdAndCustomerDto_whenUpdateEntity_thenThrowMethodArgumentNotValidException() {
        CustomerDto customerDto = MockCustomerFactory.generateMockCustomerDtos().getFirst();
        customerDto.setPhoneNumber("905322864662");
        String body = gson.toJson(customerDto);

        given()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(accessTokens.getFirst()))
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(CUSTOMER_RESOURCE_ENDPOINT, 2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("phoneNumber", equalTo(ResponseMessage.INVALID_PHONE_NUMBER));
    }

    @Test
    @Order(7)
    @DisplayName("Happy path test: Upload valid profile photo case")
    void givenIdAndMultipartFile_whenUploadProfilePhoto_thenSuccessReturnMessage() {
        MultiPartSpecification multiPartSpecification = constructMultiPartSpecification("valid_profilePhoto.png", MediaType.IMAGE_PNG_VALUE);

        given()
                .log()
                .all()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(accessTokens.get(1)))
                .multiPart(multiPartSpecification)
                .when()
                .post(PROFILE_PHOTO_UPLOAD_ENDPOINT, 3)
                .then()
                .assertThat()
                .statusCode(HttpStatus.ACCEPTED.value())
                .body("response", equalTo(ResponseMessage.FILE_UPLOAD_APPROVAL));
    }

    @Test
    @Order(8)
    @DisplayName("Exception path test: Upload invalid profile photo case")
    void givenIdAndMultipartFile_whenUploadProfilePhoto_thenReturnFailMessage() {
        MultiPartSpecification multiPartSpecification = constructMultiPartSpecification("invalid_profilePhoto.txt", MediaType.TEXT_PLAIN_VALUE);

        given()
                .log()
                .all()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(accessTokens.getFirst()))
                .multiPart(multiPartSpecification)
                .when()
                .post(PROFILE_PHOTO_UPLOAD_ENDPOINT, 2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                .body("message", equalTo(ResponseMessage.INVALID_PHOTO_CONTENT_TYPE));
    }

    @Test
    @Order(9)
    @DisplayName("Happy path test: Download profile photo case")
    void givenId_whenDownloadProfilePhoto_thenReturnFile() {
        given()
                .when()
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(accessTokens.get(1)))
                .get(PROFILE_PHOTO_DOWNLOAD_ENDPOINT, 3)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());
    }

    private void registerCustomer(RegistrationRequest request) {
        given()
                .contentType(ContentType.JSON)
                .body(gson.toJson(request))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());
    }

    private void generateAccessTokensOfCustomers() {
        List<String> usernames = MockCustomerFactory.generateMockCustomerDtos()
                .stream()
                .map(CustomerDto::getNationalId)
                .toList();

        usernames.forEach(username -> {
            String token = generateAccessTokenFromUsername(username);
            accessTokens.add(token);
        });
    }

    private String generateAccessTokenFromUsername(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return jwtService.generateTokens(userDetails).get(JwtUtil.Header.ACCESS_TOKEN_HEADER);
    }

    private void generateAgreement() {
        File file;

        try {
            file = MockFileFactory.generateMockFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File savedFile = fileRepository.save(file);

        Agreement agreement = MockAgreementFactory.getMockAgreement();
        agreement.setFiles(Set.of(savedFile));

        agreementRepository.save(agreement);
    }

    private static MultiPartSpecification constructMultiPartSpecification(String profilePhotoName, String mediaType) {
        java.io.File file = new java.io.File(PHOTO_PATH + profilePhotoName);

        try {
            return new MultiPartSpecBuilder(Files.readAllBytes(file.toPath()))
                    .fileName(file.getName())
                    .controlName("file")
                    .mimeType(mediaType)
                    .build();
        } catch (IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }
}
