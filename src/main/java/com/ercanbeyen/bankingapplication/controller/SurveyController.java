package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.SurveyUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;

    @GetMapping
    public ResponseEntity<List<SurveyDto>> getSurveys() {
        return ResponseEntity.ok(surveyService.getSurveys());
    }

    @GetMapping("/{customerNationalId}/{type}/{date}")
    public ResponseEntity<SurveyDto> getSurvey(
            @PathVariable("customerNationalId") String customerNationalId,
            @PathVariable("type") SurveyType surveyType,
            @PathVariable("date") LocalDate date) {
        return ResponseEntity.ok(surveyService.getSurvey(customerNationalId, surveyType, date));
    }

    @PostMapping
    public ResponseEntity<SurveyDto> createSurvey(@RequestBody @Valid SurveyDto request) {
        SurveyUtils.checkSurveyBeforeSave();
        return new ResponseEntity<>(surveyService.createSurvey(request), HttpStatus.CREATED);
    }

    @PutMapping("/{customerNationalId}/{type}/{date}")
    public ResponseEntity<SurveyDto> updateSurvey(
            @PathVariable("customerNationalId") String customerNationalId,
            @PathVariable("type") SurveyType surveyType,
            @PathVariable("date") LocalDate date,
            @RequestBody @Valid SurveyDto request) {
        SurveyUtils.checkSurveyBeforeSave();
        return ResponseEntity.ok(surveyService.updateSurvey(customerNationalId, surveyType, date, request));
    }
}
