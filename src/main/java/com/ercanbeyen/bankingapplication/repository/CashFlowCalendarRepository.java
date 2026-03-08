package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.model.CashFlowCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashFlowCalendarRepository extends JpaRepository<CashFlowCalendar, String> {

}
