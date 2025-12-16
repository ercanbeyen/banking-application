package com.ercanbeyen.bankingapplication.constant.message;

public final class LogMessage {
    public static final String ECHO = "Program is in class {} and method {}";
    public static final String RESOURCE_FOUND = "{} is found";
    public static final String RESOURCE_NOT_FOUND = "{} is not found";
    public static final String RESOURCE_UNIQUE = "{} is unique";
    public static final String RESOURCE_NOT_UNIQUE = "{} is not unique";
    public static final String EXCEPTION = "Exception message: {}";
    public static final String TRANSACTION_MESSAGE = "Transaction message: {}";
    public static final String SCHEDULED_TASK_STARTED = "Scheduled task is started for {}";
    public static final String SCHEDULED_TASK_ENDED = "Scheduled task is ended for {}";
    public static final String BEFORE_REQUEST = "Before sending the request";
    public static final String AFTER_REQUEST = "After sent the request";
    public static final String REST_TEMPLATE_SUCCESS = """
            Response is returned successfully after rest template call.
            Response: {}
            """;
    public static final String RESOURCE_CREATE_SUCCESS = "{} {} is successfully created";
    public static final String RESOURCE_DELETE_SUCCESS = "{} {} is successfully deleted";
    public static final String CLASS_OF_RESPONSE = "Class of response: {}";
    public static final String CLASS_OF_OBJECT = "Class of {}: {}";
    public static final String ACCOUNT_ACTIVITY_STATUS_ECHO = "Account activity, requested amount and transaction fee: {} & {} & {}";
    public static final String PROCESSED_AMOUNT = "Amount after {}: {}";
    public static final String NO_ACCOUNT_ACTIVITY_CHANGE = "No change for account activity type of {}";
    public static final String ENOUGH_BALANCE = "Balance is enough for {}";
    public static final String PAYMENT_DATE_HAS_ARRIVED = "Payment date has arrived for {}. Update the next payment date";
    public static final String ONLY_ENTITIES_ARE_GOING_TO_BE_PROCESSED = "Only {}s are going to be processed for {} {}";
    public static final String DEPOSIT_ACCOUNT_FIELDS_SHOULD_UPDATE = "Account type is deposit. So need to update deposit account related fields";
    public static final String UNUSABLE_METHOD = "Unusable method";

    public static class Batch {
        public static final String STEP_STATUS = "!!! Step {} is {} at {}";

        private Batch() {}
    }

    public static final class Test {
        public static final String UNIT = "Unit";
        public static final String SETUP = "Setup...";
        public static final String TEAR_DOWN = "Tear down...";
        private static final String TEMPLATE = "{} tests of {} are";
        public static final String START = TEMPLATE + " starting";
        public static final String END = TEMPLATE + " finishing";

        private Test() {}
    }

    private LogMessage() {}
}
