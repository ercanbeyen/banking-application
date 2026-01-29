package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.FilePreviewInfo;
import com.ercanbeyen.bankingapplication.entity.File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FileService {
    void storeFile(MultipartFile request);
    CompletableFuture<File> storeFile(MultipartFile request, String name);
    File getFile(String id);
    String deleteFile(String id);
    List<FilePreviewInfo> getFilePreviewInfos();
}
