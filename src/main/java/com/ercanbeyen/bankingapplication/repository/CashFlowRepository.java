package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.embeddable.CashFlowPK;
import com.ercanbeyen.bankingapplication.entity.CashFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, CashFlowPK> {

}
