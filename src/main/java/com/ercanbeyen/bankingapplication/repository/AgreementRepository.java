package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, String> {
    List<Agreement> findBySubject(AgreementSubject subject);
    @Query(value = """
            SELECT *
            FROM agreements a
            INNER JOIN agreement_customer ac ON a.title = ac.agreement_title
            WHERE ac.agreement_title = :title AND ac.customer_national_id = :national_id
            """, nativeQuery = true)
    Optional<Agreement> findByTitleAndCustomerNationalId(@Param("title") String title, @Param("national_id") String nationalId);
}
