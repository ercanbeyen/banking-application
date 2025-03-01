package com.ercanbeyen.bankingapplication.listener;

import com.ercanbeyen.bankingapplication.entity.BaseEntity;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseEntityListener {

    @PrePersist
    void onPrePersist(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    @PostPersist
    void onPostPersist(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    @PostLoad
    void onPostLoad(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    @PreUpdate
    void onPreUpdate(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    @PostUpdate
    void onPostUpdate(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    @PreRemove
    void onPreRemove(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    @PostRemove
    void onPostRemove(BaseEntity baseEntity) {
        logListenerInformation(LoggingUtil.getCurrentMethodName(), baseEntity);
    }

    private static void logListenerInformation(String methodName, BaseEntity baseEntity) {
        log.info("BaseEntityListener.{}(): {}", methodName, baseEntity);
    }
}
