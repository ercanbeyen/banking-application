# Banking Application
---

## Spring Boot Application
---

### Summary
It is a banking application includes basic banking services. Abstract CRUD approach is used throughout the development.<br/>

### Information
- Customers must register and then log in using Two-Factor Authentication (2FA) in order to use banking services.
- Customers can attempt to log in a maximum of 5 times consecutively in Step 1. If they fail to log in 5 times consecutively, their account will be locked for 30 minutes.
- Customers can attempt to log in a maximum of 3 times consecutively within 5 minutes using the OTP code sent to their email address in Step 2. If they fail to log in, the OTP code will be considered invalid.
- Customers cannot use their last 3 passwords during password renewal.
- Customers can transfer money to their deposit accounts using their current accounts.
- Customers can buy and sell foreign currency to the bank through current accounts at the specified buying and selling rates.
- Customers can trade within daily transaction limits and may incur transaction fees for some transactions.
- Customers can create money transfer orders for future dates.
- Customers are obliged to comply with the agreements they have approved.
- Customers will receive notifications once the transactions are successfully completed.
- Customers can participate in surveys related to their transactions.
- Customers can receive their receipts, account statements, and financial status reports via email at any time.

### Details
- Scheduled tasks run while application is running.
- News are automatically added in chunks of size 40 at the start of the application's execution.
- Roles and permissions are added at the start of the application's execution.
- Scripts inside resources/db.stored_procedure must be written into related database containers to call procedures.

### Tech Stack
---
- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Data Cassandra
- Spring Batch
- JUnit 5
- REST Assured
- Redis
- MySQL
- Cassandra
- Docker

### Prerequisites
---
- Maven
- Docker

### Build & Run & Debug
---
In order to pull images from Dockerhub, you should run the below commands
```
$ docker pull mysql
$ docker pull cassandra
$ docker pull redis
$ docker pull axllent/mailpit
```

Then, you should run the below commands in order to run the application (Default port is 8080)

1) Create jar file
2) Create the image of the application via building
3) Run the containers

```
$ mvn clean install
$ docker-compose build
$ docker-compose up
```

If you want to skip the tests while creating the jar file, you should replace the first command above with the following command

`$ mvn clean install -D skipTests`

In order to debug the application, you should follow the below steps

1) Add Remote JVM Debug Configuration (You can use the default values that come in the debug configuration)
2) Run the containers
3) Start debugging on configured port (Default port is 5005)

### Monitor
---
In order to monitor the application, you should use the following URL to get metrics you may monitor.<br/>
`http://localhost:${PORT}/actuator`
 
If you want to monitor specific metric, you should append the metric to the actuator URL. For example, in order to check health, you should use the following URL to check health.<br/>
`http://localhost:${PORT}/actuator/health`

### Mailpit Page
---

You may use Mailpit page to access the incoming emails.<br/>
You can access Mailpit page using the following URL.<br/>
`http://localhost:8025`

### API Documentation
---
You may use Swagger-UI with the port of the application you configured to access the project's API documentation.<br/>
You should use the following URL to access the Swagger-UI.<br/>
`http://localhost:${PORT}/swagger-ui.html`
