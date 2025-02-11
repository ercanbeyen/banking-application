package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, String> {
    Optional<Agreement> findBySubject(String subject);
    @Query(value = """
            SELECT *
            FROM agreements a
            INNER JOIN agreement_customer ac ON a.subject = ac.agreement_subject
            WHERE ac.agreement_subject = :subject AND ac.customer_national_id = :national_id
            """, nativeQuery = true)
    Optional<Agreement> findBySubjectAndCustomerNationalId(@Param("subject") String subject, @Param("national_id") String nationalId);
}
