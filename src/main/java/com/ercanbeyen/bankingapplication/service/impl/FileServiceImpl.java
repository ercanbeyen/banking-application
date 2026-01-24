package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.FilePreviewInfo;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.FileRepository;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.FileUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final FileRepository fileRepository;

    @Async
    @Override
    public void storeFile(MultipartFile request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        String name = StringUtils.cleanPath(Objects.requireNonNull(request.getOriginalFilename()));
        saveFile(request, name);
    }

    @Async
    @Override
    public CompletableFuture<File> storeFile(MultipartFile request, String name) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        String fileName = name + "." + FileUtil.getPlainContentTypeOfFile(request); // file extension is added
        return saveFile(request, fileName);
    }

    @Override
    public File getFile(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return findById(id);
    }

    @Override
    public String deleteFile(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.FILE.getValue();

        fileRepository.findById(id)
                .ifPresentOrElse(file -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);

                    try {
                        fileRepository.delete(file);
                    } catch (Exception exception) {
                        log.error(LogMessage.EXCEPTION, exception.getMessage());
                        String message = "File is a profile photo. So, it might only be deleted from customer api";
                        throw new ResourceExpectationFailedException(message);
                    }
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);

        return ResponseMessage.FILE_DELETE_SUCCESS;
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

    private CompletableFuture<File> saveFile(MultipartFile multipartFile, String name) {
        return CompletableFuture.supplyAsync(() -> {
            File file;

            try {
                file = new File(name, multipartFile.getContentType(), multipartFile.getBytes());
            } catch (IOException _) {
                throw new ResourceExpectationFailedException("Error occurred while processing the file");
            }

            File savedFile = fileRepository.save(file);
            log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.FILE.getValue(), savedFile.getId());

            return savedFile;
        }).exceptionally(exception -> {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            throw new ResourceExpectationFailedException(ResponseMessage.FILE_UPLOAD_ERROR);
        });
    }
}
