package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@UtilityClass
public class FileUtil {
    private final int FILE_NAME_LENGTH_THRESHOLD = 100;

    public void checkIsFileEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResourceExpectationFailedException("File should not be empty");
        }
    }

    public void checkLengthOfFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null) {
            throw new ResourceExpectationFailedException("File name should not be empty!");
        }

        int lengthOfOriginalFileName = originalFileName.length();
        log.info("File original name and its length: {} - {}", originalFileName, lengthOfOriginalFileName);

        String plainContentType = getPlainContentTypeOfFile(file);
        log.info("Plain content type: {}", plainContentType);

        int lengthOfPlainContentType = plainContentType.length();
        int endIndex = lengthOfOriginalFileName - (1 + lengthOfPlainContentType); // full stop + plain content type -> .pdf
        String plainFileName = originalFileName.substring(0, endIndex);
        log.info("Plain file name: {}", plainFileName);

        if (plainFileName.length() > FILE_NAME_LENGTH_THRESHOLD) {
            throw new ResourceExpectationFailedException("File name length threshold is exceeded. Maximum length is " + FILE_NAME_LENGTH_THRESHOLD);
        }
    }

    public List<String> getPlainContentTypes(List<String> contentTypes) {
        return contentTypes.stream()
                .map(FileUtil::getPlainContentType)
                .toList();
    }

    private String getPlainContentTypeOfFile(MultipartFile file) {
        String contentType = file.getContentType();
        assert contentType != null;

        return getPlainContentType(contentType);
    }

    private String getPlainContentType(String contentType) {
        String[] contentTypeSplitArray = contentType.split("/");
        String plainContentType = contentTypeSplitArray[1];

        if (plainContentType == null) {
            throw new ResourceConflictException("Invalid content type");
        }

        return plainContentType;
    }
}
