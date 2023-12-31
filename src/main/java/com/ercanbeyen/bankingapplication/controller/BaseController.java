package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.BaseDto;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.BaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
public abstract class BaseController<T extends BaseDto> {
    private final BaseService<T> baseService;

    @GetMapping
    public ResponseEntity<?> getEntities() {
        return new ResponseEntity<>(baseService.getEntities(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntity(@PathVariable("id") Integer id) {
        return baseService.getEntity(id)
                .map(t -> new ResponseEntity<>(t, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException(ResponseMessages.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<?> createEntity(@RequestBody @Valid T request) {
        return new ResponseEntity<>(baseService.createEntity(request), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid T request) {
        return new ResponseEntity<>(baseService.updateEntity(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntity(@PathVariable("id") Integer id) {
        return baseService.getEntity(id)
                .map(t -> {
                    baseService.deleteEntity(id);
                    return new ResponseEntity<>("Successfully deleted", HttpStatus.OK);
                })
                .orElseThrow(() -> new ResourceNotFoundException(ResponseMessages.NOT_FOUND));
    }
}
