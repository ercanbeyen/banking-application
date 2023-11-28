package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileStorageController {
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info(LogMessages.ECHO_MESSAGE, "FileStorageController", "uploadFile");
        String message = "";
        HttpStatus httpStatus;
        MessageResponse response;

        try {
            fileStorageService.storeFile(file);
            httpStatus = HttpStatus.OK;
            message = "Uploaded the file successfully: " + file.getOriginalFilename();
        } catch (Exception exception) {
            httpStatus = HttpStatus.EXPECTATION_FAILED;
            message = "Could not upload the file: " + file.getOriginalFilename() + "!";
        } finally {
            log.info("Upload operation is over");
            response = new MessageResponse(message);
        }


        return new ResponseEntity<>(response, httpStatus);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable("id") String id) {
        log.info(LogMessages.ECHO_MESSAGE, "FileStorageController", "downloadFile");
        File file = fileStorageService.getFile(id);
        String fileName = file.getName();
        log.info("file.getName(): {}", fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file.getData());
    }
}
