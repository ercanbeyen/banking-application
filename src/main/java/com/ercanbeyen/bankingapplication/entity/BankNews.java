package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "bank_news")
public non-sealed class BankNews extends News {

}
