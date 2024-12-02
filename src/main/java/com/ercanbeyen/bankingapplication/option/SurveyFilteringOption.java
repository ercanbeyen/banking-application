package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;

import java.time.LocalDate;

public record SurveyFilteringOption(String customerNationalId, AccountActivityType accountActivityType, SurveyType surveyType, LocalDate createdAt, LocalDate validUntil) {

}
