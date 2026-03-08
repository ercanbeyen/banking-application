package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.model.Customer;
import com.ercanbeyen.bankingapplication.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    void deleteAllByCustomer(Customer customer);
}
