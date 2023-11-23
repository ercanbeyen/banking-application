package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.BaseDto;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
public abstract class BaseController<T extends BaseDto> {
    private final BaseService<T> baseService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return new ResponseEntity<>(baseService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return baseService.findById(id)
                .map(t -> new ResponseEntity<>(t, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody T request) {
        return new ResponseEntity<>(baseService.create(request), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody T request) {
        return new ResponseEntity<>(baseService.update(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return baseService.findById(id)
                .map(t -> {
                    baseService.delete(id);
                    return new ResponseEntity<>("Successfully deleted", HttpStatus.OK);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }
}
