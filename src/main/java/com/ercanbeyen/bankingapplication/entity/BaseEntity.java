package com.ercanbeyen.bankingapplication.entity;


import com.ercanbeyen.bankingapplication.constant.query.Query;
import com.ercanbeyen.bankingapplication.listener.BaseEntityListener;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@EntityListeners(BaseEntityListener.class)
@Data
@MappedSuperclass
public abstract sealed class BaseEntity permits Account, Branch, Customer, Exchange, Fee, News, NewsReport, MoneyTransferOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @CreationTimestamp(source = SourceType.DB)
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", columnDefinition = Query.TIMESTAMP_DEFAULT_NOW)
    private LocalDateTime createdAt;
    @UpdateTimestamp(source = SourceType.DB)
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", columnDefinition = Query.TIMESTAMP_DEFAULT_NOW)
    private LocalDateTime updatedAt;
}
