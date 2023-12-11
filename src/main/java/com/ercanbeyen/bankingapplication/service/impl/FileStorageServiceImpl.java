package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.FileRepository;
import com.ercanbeyen.bankingapplication.service.FileStorageService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    private final FileRepository fileRepository;

    @Override
    public File storeFile(MultipartFile multipartFile) {
        log.info(LogMessages.ECHO_MESSAGE,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        File savedFile;

        try {
            File file = new File(fileName, multipartFile.getContentType(), multipartFile.getBytes());
            savedFile = fileRepository.save(file);
            log.info("file.getName(): {}", savedFile.getName());
            log.info("File is successfully stored");
        } catch (Exception exception) {
            log.error("Exception message: {}", exception.getMessage());
            String message = "Unable to upload file. Exception message: " + exception.getMessage();
            throw new ResourceExpectationFailedException(message);
        }

        return savedFile;
    }

    @Override
    public File getFile(String id) {
        log.info(LogMessages.ECHO_MESSAGE,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        return fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseMessages.NOT_FOUND));
    }

    @Override
    public void deleteFile(String id) {
        log.info(LogMessages.ECHO_MESSAGE,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        fileRepository.findById(id).ifPresentOrElse(file -> {
            log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.FILE);
            try {
                fileRepository.delete(file);
                log.info("File is successfully deleted");
            } catch (Exception exception) {
                String message = "File is a profile photo. So, it might only be deleted from customer api";
                throw new ResourceExpectationFailedException(message);
            }
        }, () -> {
            log.error(LogMessages.RESOURCE_NOT_FOUND, LogMessages.ResourceNames.FILE);
            throw new ResourceNotFoundException(ResponseMessages.NOT_FOUND);
        });
    }

    @Override
    public Stream<File> getAllFiles() {
        log.info(LogMessages.ECHO_MESSAGE,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        return fileRepository.findAll()
                .stream();
    }
}