package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.BranchOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchOrderRepository extends JpaRepository<BranchOrder, String> {
    List<BranchOrder> findByCustomer(Customer customer);
}
