package com.ercanbeyen.bankingapplication.dto;


import java.util.List;

public record AgreementDto(String id, String title, String subject, List<String> fileNames) {

}
