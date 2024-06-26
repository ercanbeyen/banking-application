package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.RatingReason;
import com.ercanbeyen.bankingapplication.dto.RatingDto;
import com.ercanbeyen.bankingapplication.dto.response.RatingStatisticsResponse;
import com.ercanbeyen.bankingapplication.service.RatingService;
import com.ercanbeyen.bankingapplication.util.RatingUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @GetMapping
    public ResponseEntity<List<RatingDto>> getRatings() {
        return ResponseEntity.ok(ratingService.getRatings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RatingDto> getRating(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ratingService.getRating(id));
    }

    @PostMapping
    public ResponseEntity<RatingDto> createRating(@RequestBody @Valid RatingDto request) {
        RatingUtils.checkRatingBeforeSave(request);
        return new ResponseEntity<>(ratingService.createRating(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RatingDto> updateRating(@PathVariable("id") UUID id, @RequestBody @Valid RatingDto request) {
        RatingUtils.checkRatingBeforeSave(request);
        return ResponseEntity.ok(ratingService.updateRating(id, request));
    }

    @GetMapping("/statistics/reasons")
    public ResponseEntity<RatingStatisticsResponse<RatingReason, Integer>> getReasonStatistics(
            @RequestParam(name = "from", required = false) Integer fromYear,
            @RequestParam(name = "to", required = false) Integer toYear,
            @RequestParam(name = "minimum-frequency", required = false, defaultValue = "1") Integer minimumFrequency) {
        RatingUtils.checkRatingStatisticsFilteringParameters(fromYear, toYear, minimumFrequency);
        return ResponseEntity.ok(ratingService.getReasonStatistics(fromYear, toYear, minimumFrequency));
    }

    @GetMapping("/statistics/rates")
    public ResponseEntity<RatingStatisticsResponse<Integer, Integer>> getRateStatistics(
            @RequestParam(name = "from", required = false) Integer fromYear,
            @RequestParam(name = "to", required = false) Integer toYear,
            @RequestParam(name = "minimum-frequency", required = false, defaultValue = "1") Integer minimumFrequency) {
        RatingUtils.checkRatingStatisticsFilteringParameters(fromYear, toYear, minimumFrequency);
        return ResponseEntity.ok(ratingService.getRateStatistics(fromYear, toYear, minimumFrequency));
    }
}
