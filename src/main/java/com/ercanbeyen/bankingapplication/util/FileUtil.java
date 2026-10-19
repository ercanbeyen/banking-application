package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@UtilityClass
public class FileUtil {
    private final int FILE_NAME_LENGTH_THRESHOLD = 100;
    public final String APPLICATION_DOC_VALUE = "application/msword";
    public final String APPLICATION_DOCX_VALUE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public FileUploadRequest createFileUploadRequest(MultipartFile request, String customFileName) {
        checkFile(request);

        try {
            byte[] data = request.getBytes();
            String name = Optional.ofNullable(customFileName).isPresent()
                    ? customFileName + "." + getFileExtension(request.getContentType()) // file extension is added
                    : request.getOriginalFilename();
            return new FileUploadRequest(name, request.getContentType(), data);
        } catch (IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    public void checkFile(MultipartFile request) {
        checkFileContent(request);
        checkLengthOfFileName(request);
    }

    public void checkFileContent(MultipartFile request) {
        if (Optional.ofNullable(request).isEmpty() || request.isEmpty()) {
            throw new ResourceExpectationFailedException("File should not be empty");
        }

        try {
            request.getBytes();
        } catch (IOException _) {
            throw new InternalServerErrorException("An error occurred while reading the file");
        }
    }

    public String getFileExtension(String contentType) {
        if (StringUtil.isBlank(contentType)) {
            throw new ResourceExpectationFailedException("Invalid content type");
        }

        return extractFileExtension(contentType);
    }

    private void checkLengthOfFileName(MultipartFile request) {
        String originalFileName = request.getOriginalFilename();

        if (Optional.ofNullable(originalFileName).isEmpty()) {
            throw new ResourceExpectationFailedException("File name should not be empty!");
        }

        int lengthOfOriginalFileName = originalFileName.length();
        log.info("File original name and its length: {} - {}", originalFileName, lengthOfOriginalFileName);

        String fileExtension = extractFileExtension(Objects.requireNonNull(request.getContentType()));
        log.info("File extension: {}", fileExtension);

        int lengthOfFileExtension = fileExtension.length();
        int endIndex = lengthOfOriginalFileName - (1 + lengthOfFileExtension); // full stop + file extension -> .pdf
        String plainFileName = originalFileName.substring(0, endIndex);
        log.info("Plain file name: {}", plainFileName);

        if (plainFileName.length() > FILE_NAME_LENGTH_THRESHOLD) {
            throw new ResourceExpectationFailedException("File name length threshold is exceeded. Maximum length is " + FILE_NAME_LENGTH_THRESHOLD);
        }
    }

    private String extractFileExtension(String contentType) {
        return switch (contentType) {
            case APPLICATION_DOC_VALUE -> "doc";
            case APPLICATION_DOCX_VALUE -> "docx";
            default -> {
                String[] contentTypeSplitArray = contentType.split("/");
                String plainContentType = contentTypeSplitArray[1];

                if (Optional.ofNullable(plainContentType).isEmpty()) {
                    throw new ResourceExpectationFailedException("Invalid content type");
                }

                yield plainContentType;
            }
        };
    }
}
