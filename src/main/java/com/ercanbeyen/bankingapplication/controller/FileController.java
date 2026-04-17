package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.response.FilePreview;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("id") String id) {
        File file = fileService.getFile(id);
        byte[] data = file.getData();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getType()));
        headers.setContentLength(data.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getName())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteFile(@PathVariable("id") String id) {
        fileService.deleteFile(id);
        MessageResponse<String> response = new MessageResponse<>(ResponseMessage.FILE_DELETE_SUCCESS);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/previews")
    public ResponseEntity<List<FilePreview>> getFilePreviews() {
        List<FilePreview> filePreviews = fileService.getFilePreviewInfos()
                .stream()
                .map(filePreviewInfo -> {
                    String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/v1/files/")
                            .path(filePreviewInfo.id())
                            .toUriString();

                    return new FilePreview(
                            filePreviewInfo.name(),
                            fileDownloadUri,
                            filePreviewInfo.type(),
                            filePreviewInfo.size()
                    );
                })
                .toList();

        return ResponseEntity.ok(filePreviews);
    }
}
