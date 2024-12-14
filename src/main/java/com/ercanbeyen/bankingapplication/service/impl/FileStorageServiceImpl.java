package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.FileRepository;
import com.ercanbeyen.bankingapplication.service.FileStorageService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    private final FileRepository fileRepository;

    @Async
    @Override
    public CompletableFuture<File> storeFile(MultipartFile multipartFile) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));

        return CompletableFuture.supplyAsync(() -> {
            File file;

            try {
                file = new File(fileName, multipartFile.getContentType(), multipartFile.getBytes());
            } catch (IOException exception) {
                throw new ResourceExpectationFailedException("Error occurred in method getBytes");
            }

            File savedFile = fileRepository.save(file);
            log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.FILE.getValue(), savedFile.getId());

            return savedFile;
        }).exceptionally(exception -> {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            throw new ResourceExpectationFailedException(ResponseMessage.FILE_UPLOAD_ERROR);
        });
    }

    @Override
    public File getFile(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return findById(id);
    }

    @Override
    public String deleteFile(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        fileRepository.findById(id).ifPresentOrElse(file -> {
            log.info(LogMessage.RESOURCE_FOUND, Entity.FILE.getValue());

            try {
                fileRepository.delete(file);
            } catch (Exception exception) {
                log.error(LogMessage.EXCEPTION, exception.getMessage());
                String message = "File is a profile photo. So, it might only be deleted from customer api";
                throw new ResourceExpectationFailedException(message);
            }
        }, () -> {
            log.error(LogMessage.RESOURCE_NOT_FOUND, Entity.FILE.getValue());
            throw new ResourceNotFoundException(ResponseMessage.NOT_FOUND);
        });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.FILE.getValue(), id);

        return ResponseMessage.FILE_DELETE_SUCCESS;
    }

    @Override
    public Stream<File> getAllFiles() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return fileRepository.findAll()
                .stream();
    }

    private File findById(String id) {
        String entity = Entity.FILE.getValue();
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return file;
    }
}
