package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public sealed class News extends BaseEntity permits BankNews, OfferNews {
    private String title;
    private String url;
}
