package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@UtilityClass
public class PhotoUtil {
    private final List<String> validContentTypes = List.of(
            "image/png", "image/jpg", "image/jpeg"
    );

    public void checkPhoto(MultipartFile file) {
        FileUtil.checkIsFileEmpty(file);
        checkContentTypeOfPhoto(file);
        FileUtil.checkLengthOfFileName(file);
    }

    public List<String> getPlainContentTypes() {
        return FileUtil.getPlainContentTypes(validContentTypes);
    }

    private void checkContentTypeOfPhoto(MultipartFile file) {
        if (!validContentTypes.contains(file.getContentType())) {
            throw new ResourceExpectationFailedException(ResponseMessage.INVALID_PHOTO_CONTENT_TYPE);
        }
    }
}
