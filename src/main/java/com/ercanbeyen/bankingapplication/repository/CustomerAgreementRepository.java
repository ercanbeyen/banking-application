package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAgreementRepository extends JpaRepository<CustomerAgreement, String> {
    List<CustomerAgreement> findByCustomer(Customer customer);
    boolean existsByAgreementTitleAndCustomerNationalId(String title, String nationalId);
    @Modifying
    @Query("""
           DELETE
           FROM CustomerAgreement ca
           WHERE ca.agreement.title = :title
           """)
    void deleteAllByAgreementTitle(@Param("title") String title);
}
