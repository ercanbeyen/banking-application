package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.dto.response.FileResponse;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;

    @PostMapping
    public ResponseEntity<MessageResponse<String>> uploadFile(@RequestParam("file") MultipartFile request) {
        FileUtil.checkFile(request);
        fileService.storeFile(request);
        MessageResponse<String> response = new MessageResponse<>(ResponseMessage.FILE_UPLOAD_SUCCESS);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("id") String id) {
        File file = fileService.getFile(id);
        String fileName = file.getName();
        log.info("file.getName(): {}", fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file.getData());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteFile(@PathVariable("id") String id) {
        MessageResponse<String> response = new MessageResponse<>(fileService.deleteFile(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> getFileList() {
        List<FileResponse> fileResponseList = fileService.getAllFiles()
                .map(file -> {
                    String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/v1/files")
                            .path(file.getId())
                            .toUriString();

                    return new FileResponse(
                            file.getName(),
                            fileDownloadUri,
                            file.getType(),
                            file.getData().length
                    );
                })
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(fileResponseList);
    }
}
