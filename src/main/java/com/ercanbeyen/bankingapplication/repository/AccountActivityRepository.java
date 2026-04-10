package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountActivityRepository extends JpaRepository<AccountActivity, String> {
    List<AccountActivity> findBySenderAccountIdOrRecipientAccountId(Integer senderAccountId, Integer recipientAccountId);
    List<AccountActivity> findBySenderAccountId(Integer senderAccountId);
    List<AccountActivity> findByRecipientAccountId(Integer recipientAccountId);
}
