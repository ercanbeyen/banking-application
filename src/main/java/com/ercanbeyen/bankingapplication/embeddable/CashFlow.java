package com.ercanbeyen.bankingapplication.embeddable;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Embeddable
public class CashFlow {
    private String explanation;
    private LocalDate date;
}
