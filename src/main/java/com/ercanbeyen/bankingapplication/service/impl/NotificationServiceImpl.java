package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.Notification;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.NotificationMapper;
import com.ercanbeyen.bankingapplication.repository.NotificationRepository;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CustomerService customerService;

    @Async
    @Override
    public CompletableFuture<NotificationDto> createNotification(NotificationDto notificationDto) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return CompletableFuture.supplyAsync(() -> {
            Notification notification = notificationMapper.dtoToEntity(notificationDto);
            Customer customer = customerService.findByNationalId(notificationDto.customerNationalId());
            notification.setCustomer(customer);

            Notification savedNotification = notificationRepository.save(notification);
            log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.NOTIFICATION.getValue(), savedNotification.getId());

            return notificationMapper.entityToDto(savedNotification);
        });
    }

    @Override
    public String deleteNotification(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.NOTIFICATION.getValue();

        notificationRepository.findById(id)
                .ifPresentOrElse(notification -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    notificationRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);

        return entity + " " + id + " is successfully deleted";
    }

    @Transactional
    @Override
    public void deleteNotifications(String nationalId) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = customerService.findByNationalId(nationalId);
        log.info(LogMessage.RESOURCE_FOUND, Entity.CUSTOMER.getValue());

        notificationRepository.deleteAllByCustomer(customer);
    }
}
