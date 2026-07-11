package com.ercanbeyen.bankingapplication.dto.request;

public record FileUploadRequest(String name, String contentType, byte[] data) {

}
