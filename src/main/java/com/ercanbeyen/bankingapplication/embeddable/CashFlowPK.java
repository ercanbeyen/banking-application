package com.ercanbeyen.bankingapplication.embeddable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CashFlowPK implements Serializable {
    private String accountActivityId;
    private String customerNationalId;

    @Override
    public int hashCode() {
        return Objects.hash(accountActivityId, customerNationalId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof CashFlowPK cashFlowPK)) {
            return false;
        }

        return accountActivityId.equals(cashFlowPK.accountActivityId) && customerNationalId.equals(cashFlowPK.getCustomerNationalId());
    }
}
