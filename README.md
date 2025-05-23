# Banking Application
---

## Spring Boot Application
---

### Summary
It is a banking application includes basic banking operations. Abstract crud approach is used throughout the development.<br/>
There are 16 entities in this project

Entities:
- Customer
- Account
- File
- Notification
- Account Activity
- Transfer Order
- News Report
- News
- Survey
- Exchange
- Branch
- Daily Activity Limit
- Charge
- Fee
- Cash Flow Calendar
- Agreement

### Requirements
- Customer's national id, email and phone number must be unique.
- If account is deposit, then it must also have deposit period and interest ratio.
- News Report is used to add news.
- Customer can buy and sell foreign currency to the bank through current accounts at the specified buying and selling rates.
- Customer can perform transactions within daily transaction limits and may pay transaction fees for some transactions.
- Customer can create one-time or regular transfer orders to transfer money at future dates.
- Customer is obliged to comply with the agreements s/he has approved.

### Additionals
- Scheduled tasks run while application is running.
- News are automatically added in chunks of size 40.
- Notifications are created to inform the customer that the relevant transaction has been completed successfully.
- Customers can participate in surveys related to their transactions.
- Customer can generate a PDF formatted receipt for a specific account activity at any time.
- Scripts inside resources/db.stored_procedure must be written into related database containers to call procedures.

### Tech Stack
---
- Java 21
- Spring Boot
- Spring Data JPA
- Spring Data Cassandra
- Spring Batch
- JUnit 5
- Rest-assured
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
In order to monitor the application, you should use below actuator url to get metrics you may monitor.<br/>
`http://localhost:${PORT}/actuator`
 
If you want to monitor specific metric, you should append the metric to the actuator url. For example, in order to check health, you should use the below url to check health.<br/>
`http://localhost:${PORT}/actuator/health`

### Api Documentation
---
You may use Swagger-UI with the port of the application you configured to access the project's api documentation.<br/>
You should use the below url to access the Swagger-UI.<br/>
`http://localhost:${PORT}/swagger-ui.html`
