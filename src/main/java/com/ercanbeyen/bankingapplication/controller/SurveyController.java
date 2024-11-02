package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.SurveyUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;

    @GetMapping
    public ResponseEntity<List<SurveyDto>> getSurveys() {
        return ResponseEntity.ok(surveyService.getSurveys());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyDto> getSurvey(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(surveyService.getSurvey(id));
    }

    @PostMapping
    public ResponseEntity<SurveyDto> createSurvey(@RequestBody @Valid SurveyDto request) {
        SurveyUtils.checkSurveyBeforeSave();
        return new ResponseEntity<>(surveyService.createSurvey(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyDto> updateSurvey(@PathVariable("id") UUID id, @RequestBody @Valid SurveyDto request) {
        SurveyUtils.checkSurveyBeforeSave();
        return ResponseEntity.ok(surveyService.updateSurvey(id, request));
    }
}
