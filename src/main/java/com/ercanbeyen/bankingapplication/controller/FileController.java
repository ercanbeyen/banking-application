package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.dto.response.FilePreview;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {
    private final FileService fileService;

    @PostMapping(value = "upload/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse<String>> uploadFile(@RequestParam("file") MultipartFile request) {
        FileUtil.checkFile(request);
        fileService.storeFile(request);
        MessageResponse<String> response = new MessageResponse<>(ResponseMessage.FILE_UPLOAD_SUCCESS);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Multiple file upload")
    public ResponseEntity<MessageResponse<String>> uploadFiles(@RequestParam("files") MultipartFile[] request) {
        for (MultipartFile file : request) {
            if (file.isEmpty()) {
                log.warn("{} {} is empty!", Entity.FILE.getValue(), file.getName());
                continue;
            }

            List<FileUploadRequest> filesToUpload = new ArrayList<>();

            try {
                FileUploadRequest fileToUpload = new FileUploadRequest(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()
                );

                filesToUpload.add(fileToUpload);
            } catch (IOException _) {
                MessageResponse<String> response = new MessageResponse<>("An error occurred while reading the files.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            fileService.storeFiles(filesToUpload);
        }

        MessageResponse<String> response = new MessageResponse<>("Files were successfully retrieved, and the upload process has started in the background.");
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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

    @DeleteMapping("/{id}/delete")
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
