package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Channel;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "account_activities")
@NoArgsConstructor
public class AccountActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Enumerated(EnumType.STRING)
    private AccountActivityType type;
    @ManyToOne
    @JoinColumn(name = "sender_account_id", referencedColumnName = "id")
    private Account senderAccount;
    @ManyToOne
    @JoinColumn(name = "recipient_account_id", referencedColumnName = "id")
    private Account recipientAccount;
    private Double amount;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> summary;
    private String explanation;
    @Enumerated(EnumType.STRING)
    private Channel channel;
    private LocalDateTime createdAt;

    public AccountActivity(AccountActivityType type, Account senderAccount, Account recipientAccount, Double amount, Map<String, Object> summary, String explanation, Channel channel) {
        this.type = type;
        this.senderAccount = senderAccount;
        this.recipientAccount = recipientAccount;
        this.amount = amount;
        this.summary = summary;
        this.explanation = explanation;
        this.channel = channel;
        this.createdAt = TimeUtil.getTurkeyDateTime();
    }
}
