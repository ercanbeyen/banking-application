package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {
    Optional<Contract> findBySubject(String subject);
    @Query(value = """
            SELECT *
            FROM contracts c
            INNER JOIN contract_customer cc ON c.subject = cc.contract_subject
            WHERE cc.contract_subject = :subject AND cc.customer_national_id = :national_id
            """, nativeQuery = true)
    Optional<Contract> findBySubjectAndCustomerNationalId(@Param("subject") String subject, @Param("national_id") String nationalId);
}
