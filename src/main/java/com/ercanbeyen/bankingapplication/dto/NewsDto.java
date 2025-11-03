package com.ercanbeyen.bankingapplication.dto;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public non-sealed class NewsDto extends BaseDto {
    private String title;
    private String url;
}
