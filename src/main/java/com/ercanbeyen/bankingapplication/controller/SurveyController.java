package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.dto.response.SurveyStatisticsResponse;
import com.ercanbeyen.bankingapplication.dto.option.SurveyFilteringOption;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.SurveyUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    public ResponseEntity<List<SurveyDto>> getSurveys(SurveyFilteringOption filteringOption) {
        return ResponseEntity.ok(surveyService.getSurveys(filteringOption));
    }

    @PreAuthorize("hasAuthority('READ_DATA') OR #customerNationalId == authentication.principal.username")
    @GetMapping("/customers/{customer-national-id}")
    public ResponseEntity<SurveyDto> getSurvey(
            @PathVariable("customer-national-id") @P("customerNationalId") String customerNationalId,
            @RequestParam("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt) {
        return ResponseEntity.ok(surveyService.getSurvey(customerNationalId, accountActivityId, createdAt, surveyType));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    public ResponseEntity<SurveyDto> createSurvey(@RequestBody @Valid SurveyDto request) {
        SurveyUtil.checkRequestBeforeSave(request);
        return new ResponseEntity<>(surveyService.createSurvey(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/customers/{customer-national-id}/account-activities/{account-activity-id}")
    public ResponseEntity<SurveyDto> updateSurvey(
            @PathVariable("customer-national-id") String customerNationalId,
            @PathVariable("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt,
            @RequestBody @Valid SurveyDto request) {
        SurveyUtil.checkRequestBeforeSave(request);
        return ResponseEntity.ok(surveyService.updateSurvey(customerNationalId, accountActivityId, createdAt, surveyType, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/customers/{customer-national-id}/account-activities/{account-activity-id}")
    public ResponseEntity<Void> deleteSurvey(
            @PathVariable("customer-national-id") String customerNationalId,
            @PathVariable("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt) {
        surveyService.deleteSurvey(customerNationalId, accountActivityId, createdAt, surveyType);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("#customerNationalId == authentication.principal.username")
    @PatchMapping("/customers/{customer-national-id}/account-activities/{account-activity-id}/evaluation")
    public ResponseEntity<String> fillOutSurvey(
            @PathVariable("customer-national-id") @P("customerNationalId") String customerNationalId,
            @PathVariable("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt,
            @RequestBody @Valid SurveyDto request) {
        SurveyUtil.checkEvaluation(request);
        return ResponseEntity.ok(surveyService.fillOutSurvey(customerNationalId, accountActivityId, createdAt, surveyType, request));
    }

    @PreAuthorize("hasAuthority('READ-DATA')")
    @GetMapping("/customers/{customer-national-id}/statistics")
    public ResponseEntity<SurveyStatisticsResponse<Integer, Integer>> getSurveyStatistics(
            @PathVariable("customer-national-id") String customerNationalId,
            @RequestParam("account-activity-id") String accountActivityId,
            @RequestParam("type") SurveyType surveyType,
            @RequestParam("created-at") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS") LocalDateTime createdAt,
            @RequestParam(value = "minimum-frequency", required = false, defaultValue = "0") Integer minimumFrequency) {
        SurveyUtil.checkStatisticsParameters(createdAt, minimumFrequency);
        return ResponseEntity.ok(surveyService.getSurveyStatistics(customerNationalId, accountActivityId, createdAt, surveyType, minimumFrequency));
    }
}
