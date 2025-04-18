package com.ercanbeyen.bankingapplication.entity;


import com.ercanbeyen.bankingapplication.constant.query.Query;
import com.ercanbeyen.bankingapplication.listener.BaseEntityListener;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@EntityListeners(BaseEntityListener.class)
@Data
@MappedSuperclass
public abstract sealed class BaseEntity permits Account, Branch, Customer, Exchange, Fee, News, NewsReport, TransferOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", columnDefinition = Query.GET_NOW_TIMESTAMP)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", columnDefinition = Query.GET_NOW_TIMESTAMP)
    private LocalDateTime updatedAt;
}
