package com.ercanbeyen.bankingapplication.integration.controller;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.factory.MockAgreementFactory;
import com.ercanbeyen.bankingapplication.factory.MockCustomerFactory;
import com.ercanbeyen.bankingapplication.factory.MockFileFactory;
import com.ercanbeyen.bankingapplication.repository.AgreementRepository;
import com.ercanbeyen.bankingapplication.repository.FileRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerControllerTest {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"));
    @Container
    @ServiceConnection
    private static final CassandraContainer<?> cassandraContainer = new CassandraContainer<>(DockerImageName.parse("cassandra:latest"));
    @Container
    @ServiceConnection
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
    private static final String PHOTOS_LOCATION = "C:\\Users\\ercanbeyen\\Photos\\Banking-App\\Source\\Test\\Resources\\";
    @LocalServerPort
    private Integer port;
    @Autowired
    private Gson gson;
    @Autowired
    private AgreementRepository agreementRepository;
    @Autowired
    private FileRepository fileRepository;

    private static final String CUSTOMER_COLLECTION_ENDPOINT = "/api/v1/customers";
    public static final String CUSTOMER_RESOURCE_ENDPOINT = CUSTOMER_COLLECTION_ENDPOINT + "/{id}";

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
        given()
                .when()
                .get(CUSTOMER_COLLECTION_ENDPOINT)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("size()", is(0));
    }

    @Test
    @Order(2)
    @DisplayName("Happy path test: Create customer case")
    void givenCustomerDto_whenCreateEntity_thenReturnCustomerDto() {
        generateAgreement();

        CustomerDto request = MockCustomerFactory.generateMockCustomerDtos().getFirst();
        request.setId(null);
        generateCustomer(request);

        request = MockCustomerFactory.generateMockCustomerDtos().get(1);
        request.setId(null);
        generateCustomer(request);

        request = MockCustomerFactory.generateMockCustomerDtos().getLast();
        request.setId(null);
        generateCustomer(request);
    }

    @Test
    @Order(3)
    @DisplayName("Happy path test: Get customers case with birth date")
    void givenBirthDate_whenGetEntities_thenReturnCustomerDtos() {
        given()
                .queryParam("birthDate", String.valueOf(MockCustomerFactory.generateMockCustomers().getFirst().getBirthDate()))
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
                .when()
                .get(CUSTOMER_RESOURCE_ENDPOINT, 1)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("nationalId", equalTo(MockCustomerFactory.generateMockCustomers().getFirst().getNationalId()));
    }

    @Test
    @Order(5)
    @DisplayName("Exception path test: Get customer case")
    void givenId_whenGetEntity_thenThrowResourceNotFoundException() {
        given()
                .when()
                .get(CUSTOMER_RESOURCE_ENDPOINT, 25)
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @Order(6)
    @DisplayName("Exception path test: Update customer case")
    void givenIdAndCustomerDto_whenUpdateEntity_thenThrowMethodArgumentNotValidException() {
        CustomerDto customerDto = MockCustomerFactory.generateMockCustomerDtos().getFirst();
        customerDto.setPhoneNumber("905322864662");
        String body = gson.toJson(customerDto);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(CUSTOMER_RESOURCE_ENDPOINT, 1)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("phoneNumber", equalTo(ResponseMessage.INVALID_PHONE_NUMBER));
    }

    @Test
    @Order(7)
    @DisplayName("Happy path test: Upload valid profile photo case")
    void givenIdAndMultipartFile_whenUploadProfilePhoto_thenSuccessReturnMessage() throws IOException {
        MultiPartSpecification multiPartSpecification = constructMultiPartSpecification("valid_profilePhoto.png", MediaType.IMAGE_PNG_VALUE);

        given()
                .log()
                .all()
                .multiPart(multiPartSpecification)
                .when()
                .post(CUSTOMER_RESOURCE_ENDPOINT, 2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("response", equalTo(ResponseMessage.FILE_UPLOAD_SUCCESS));
    }

    @Test
    @Order(8)
    @DisplayName("Exception path test: Upload invalid profile photo case")
    void givenIdAndMultipartFile_whenUploadProfilePhoto_thenReturnFailMessage() throws IOException {
        MultiPartSpecification multiPartSpecification = constructMultiPartSpecification("invalid_profilePhoto.txt", MediaType.TEXT_PLAIN_VALUE);

        given()
                .log()
                .all()
                .multiPart(multiPartSpecification)
                .when()
                .post(CUSTOMER_RESOURCE_ENDPOINT, 2)
                .then()
                .assertThat()
                .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                .body("message", equalTo(ResponseMessage.INVALID_PHOTO_CONTENT_TYPE));
    }

    private static MultiPartSpecification constructMultiPartSpecification(String profilePhotoName, String mediaType) throws IOException {
        java.io.File file = new java.io.File(PHOTOS_LOCATION + profilePhotoName);
        return new MultiPartSpecBuilder(Files.readAllBytes(file.toPath()))
                .fileName(file.getName())
                .controlName("file")
                .mimeType(mediaType)
                .build();
    }

    @Test
    @Order(9)
    @DisplayName("Happy path test: Download profile photo case")
    void givenId_whenDownloadProfilePhoto_thenReturnFile() {
        given()
                .when()
                .get(CUSTOMER_RESOURCE_ENDPOINT + "/photo", 2)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    private void generateCustomer(CustomerDto request) {
        String body = gson.toJson(request);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(CUSTOMER_COLLECTION_ENDPOINT)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value())
                .body("nationalId", equalTo(request.getNationalId()));
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
        agreement.setFile(savedFile);

        agreementRepository.save(agreement);
    }
}
