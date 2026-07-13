package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@UtilityClass
public class PhotoUtil {
    private final List<String> validContentTypes = List.of(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE);

    public void checkPhoto(MultipartFile request) {
        FileUtil.checkFileContent(request);
        checkContentTypeOfPhoto(request);
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
