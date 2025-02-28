package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.entity.File;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface FileService {
    void storeFile(MultipartFile request);
    CompletableFuture<File> storeFile(MultipartFile request, String name);
    File getFile(String id);
    String deleteFile(String id);
    Stream<File> getAllFiles();
}
