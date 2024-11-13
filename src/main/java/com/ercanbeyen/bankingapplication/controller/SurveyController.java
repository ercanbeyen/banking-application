package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.dto.response.SurveyStatisticsResponse;
import com.ercanbeyen.bankingapplication.option.SurveyFilteringOptions;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.SurveyUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;

    @GetMapping
    public ResponseEntity<List<SurveyDto>> getSurveys(SurveyFilteringOptions filteringOptions) {
        return ResponseEntity.ok(surveyService.getSurveys(filteringOptions));
    }

    @GetMapping("/customers/{customer-national-id}")
    public ResponseEntity<SurveyDto> getSurvey(
            @PathVariable("customer-national-id") String customerNationalId,
            @RequestParam("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt) {
        return ResponseEntity.ok(surveyService.getSurvey(customerNationalId, accountActivityId, createdAt, surveyType));
    }

    @PostMapping
    public ResponseEntity<SurveyDto> createSurvey(@RequestBody @Valid SurveyDto request) {
        SurveyUtils.checkRequestBeforeSave(request);
        return new ResponseEntity<>(surveyService.createSurvey(request), HttpStatus.CREATED);
    }

    @PutMapping("/customers/{customer-national-id}/account-activities/{account-activity-id}")
    public ResponseEntity<SurveyDto> updateSurvey(
            @PathVariable("customer-national-id") String customerNationalId,
            @PathVariable("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt,
            @RequestBody @Valid SurveyDto request) {
        SurveyUtils.checkRequestBeforeSave(request);
        return ResponseEntity.ok(surveyService.updateSurvey(customerNationalId, accountActivityId, createdAt, surveyType, request));
    }

    @DeleteMapping("/customers/{customer-national-id}/account-activities/{account-activity-id}")
    public ResponseEntity<Void> deleteSurvey(
            @PathVariable("customer-national-id") String customerNationalId,
            @PathVariable("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt) {
        surveyService.deleteSurvey(customerNationalId, accountActivityId, createdAt, surveyType);
        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/customers/{customer-national-id}/account-activities/{account-activity-id}")
    public ResponseEntity<SurveyDto> updateValidationTime(
            @PathVariable("customer-national-id") String customerNationalId,
            @PathVariable("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt,
            @RequestParam("valid-until") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime request) {
        return ResponseEntity.ok(surveyService.updateValidationTime(customerNationalId, accountActivityId, createdAt, surveyType, request));
    }

    @GetMapping("/customers/{customer-national-id}/statistics")
    public ResponseEntity<SurveyStatisticsResponse<Integer, Integer>> getSurveyStatistics(
            @PathVariable("customer-national-id") String customerNationalId,
            @RequestParam("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt,
            @RequestParam(value = "minimum-frequency", required = false, defaultValue = "0") Integer minimumFrequency) {
        SurveyUtils.checkStatisticsParameters(createdAt, minimumFrequency);
        return ResponseEntity.ok(surveyService.getSurveyStatistics(customerNationalId, accountActivityId, createdAt, surveyType, minimumFrequency));
    }
}
