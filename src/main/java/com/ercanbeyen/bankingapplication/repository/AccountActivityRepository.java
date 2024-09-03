package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountActivityRepository extends JpaRepository<AccountActivity, String> {
    List<AccountActivity> findBySenderAccountIdOrReceiverAccountId(Integer senderAccountId, Integer receiverAccountId);
    List<AccountActivity> findBySenderAccountId(Integer senderAccountId);
    List<AccountActivity> findByReceiverAccountId(Integer receiverAccountId);
}
