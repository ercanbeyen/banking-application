package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.FilePreviewInfo;
import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.entity.File;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FileService {
    CompletableFuture<File> saveFile(FileUploadRequest fileUploadRequest);
    CompletableFuture<List<File>> saveFiles(List<FileUploadRequest> request);
    File getFile(String id);
    void deleteFile(String id);
    List<FilePreviewInfo> getFilePreviewInfos();
}
