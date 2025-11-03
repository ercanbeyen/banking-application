package com.ercanbeyen.bankingapplication.embeddable;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class RegisteredRecipient {
    private Integer accountId;
    private String fullName;
}
