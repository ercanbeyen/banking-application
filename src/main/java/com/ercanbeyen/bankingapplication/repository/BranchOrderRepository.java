package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.BranchOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchOrderRepository extends JpaRepository<BranchOrder, String> {
    List<BranchOrder> findByStatus(BranchOrderStatus status);
    List<BranchOrder> findByBranchAndStatus(Branch branch, BranchOrderStatus status);
    List<BranchOrder> findByCustomerAndStatus(Customer customer, BranchOrderStatus status);
    List<BranchOrder> findByBranchAndCustomerAndStatus(Branch branch, Customer customer, BranchOrderStatus status);
}
