package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.FilePreviewInfo;
import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.FileRepository;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final FileRepository fileRepository;

    @Async
    @Override
    public CompletableFuture<File> saveFile(FileUploadRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return CompletableFuture.supplyAsync(() -> {
                File file = new File(request.name(), request.contentType(), request.data());
                return fileRepository.save(file);
        }).exceptionally(exception -> {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            throw new ResourceExpectationFailedException(exception.getMessage());
        });
    }

    @Async
    @Override
    public CompletableFuture<List<File>> saveFiles(List<FileUploadRequest> fileUploadRequests) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        List<File> files = new ArrayList<>();

        for (FileUploadRequest fileUploadRequest : fileUploadRequests) {
            String entity = Entity.FILE.getValue();

            try {
                File file = new File(fileUploadRequest.name(), fileUploadRequest.contentType(), fileUploadRequest.data());
                File savedFile = fileRepository.save(file);
                files.add(savedFile);
                log.info("The {} was successfully saved in the background: {}", entity, file.getName());
            }
            catch (Exception exception) {
                log.error("While saving the {} {} in the background an error occurred: {}", entity, fileUploadRequest.name(), exception.getMessage());
            }
        }

        log.info("Multiple file saves are completed!");

        return CompletableFuture.completedFuture(files);
    }

    @Override
    public File getFile(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return findById(id);
    }

    @Override
    public void deleteFile(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.FILE.getValue();

        fileRepository.findById(id)
                .ifPresentOrElse(file -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);

                    try {
                        fileRepository.delete(file);
                    } catch (Exception exception) {
                        log.error(LogMessage.EXCEPTION, exception.getMessage());
                        String message = entity + " is a profile photo. So, it might only be deleted from " + Entity.CUSTOMER.getValue() + " API";
                        throw new ResourceExpectationFailedException(message);
                    }
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    @Override
    public List<FilePreviewInfo> getFilePreviewInfos() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return fileRepository.findAllPreviewInfos();
    }

    private File findById(String id) {
        String entity = Entity.FILE.getValue();
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return file;
    }
}
